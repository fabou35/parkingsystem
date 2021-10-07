package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
	public void testParkingACar() {
		// GIVEN
		Connection ticketConnection = null;
		Connection parkingConnection = null;
		ResultSet rsTicketSaved = null;
		String resultParkingAvailable = "";
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		// WHEN
		// retrieves the saved ticket in the database
		try {
			ticketConnection = ticketDAO.dataBaseConfig.getConnection();
			PreparedStatement ps = ticketConnection.prepareStatement(DBConstants.GET_TICKET);
			ps.setString(1, inputReaderUtil.readVehicleRegistrationNumber());
			rsTicketSaved = ps.executeQuery();
			ticketDAO.dataBaseConfig.closePreparedStatement(ps);
			ticketDAO.dataBaseConfig.closeResultSet(rsTicketSaved);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ticketDAO.dataBaseConfig.closeConnection(ticketConnection);
		}

		// retrieves the parking spot in the ticket and searches it in the parking database
		try {
			ticketConnection = ticketDAO.dataBaseConfig.getConnection();
			PreparedStatement ps = ticketConnection
					.prepareStatement("select PARKING_NUMBER from ticket where VEHICLE_REG_NUMBER = ?");
			ps.setString(1, inputReaderUtil.readVehicleRegistrationNumber());
			ResultSet rs = ps.executeQuery();
			rs.next();
			parkingConnection = parkingSpotDAO.dataBaseConfig.getConnection();
			ps = parkingConnection.prepareStatement("select AVAILABLE from parking where PARKING_NUMBER = ?");
			ps.setString(1, rs.getString(1));
			ResultSet rsParkingAvailable = ps.executeQuery();
			rsParkingAvailable.next();
			resultParkingAvailable = rsParkingAvailable.getString(1);

			parkingSpotDAO.dataBaseConfig.closePreparedStatement(ps);
			parkingSpotDAO.dataBaseConfig.closeResultSet(rsParkingAvailable);

			ticketDAO.dataBaseConfig.closePreparedStatement(ps);
			ticketDAO.dataBaseConfig.closeResultSet(rs);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ticketDAO.dataBaseConfig.closeConnection(ticketConnection);
			parkingSpotDAO.dataBaseConfig.closeConnection(parkingConnection);
		}

		// THEN
		assertThat(rsTicketSaved).isNotEqualTo(null);
		assertThat(resultParkingAvailable).isEqualTo("0");
	}

    @Test
    public void testParkingLotExit(){
    	// GIVEN
    	Connection conTicket = null;
    	String rsOutTimeAvailable = null;
    	String rsPriceAvailable = "0";
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        // out_time = in_time + 5 seconds
        try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        parkingService.processExitingVehicle();
        
        // WHEN
        try {
			// retrieves the out_time in the database
			conTicket = ticketDAO.dataBaseConfig.getConnection();
			PreparedStatement psTicket = conTicket.prepareStatement("select OUT_TIME from ticket "
					+ "where VEHICLE_REG_NUMBER = ?");
			psTicket.setString(1, inputReaderUtil.readVehicleRegistrationNumber());
			ResultSet rsTicket = psTicket.executeQuery();
			rsTicket.next();
			rsOutTimeAvailable = rsTicket.getString(1);
			ticketDAO.dataBaseConfig.closePreparedStatement(psTicket);
			ticketDAO.dataBaseConfig.closeResultSet(rsTicket);
			
			// retrieves the fare in the database
			PreparedStatement psPrice = conTicket.prepareStatement("select PRICE from ticket "
					+ "where VEHICLE_REG_NUMBER = ?");
			psPrice.setString(1, inputReaderUtil.readVehicleRegistrationNumber());
			ResultSet rsPrice = psPrice.executeQuery();
			rsPrice.next();
			rsPriceAvailable = rsPrice.getString(1);
			ticketDAO.dataBaseConfig.closePreparedStatement(psPrice);
			ticketDAO.dataBaseConfig.closeResultSet(rsPrice);
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
			ticketDAO.dataBaseConfig.closeConnection(conTicket);
		}
        
        // THEN
        assertThat(rsOutTimeAvailable).isNotEqualTo(null);
        assertThat(rsPriceAvailable).isEqualTo("0.0");
        
    }

}
