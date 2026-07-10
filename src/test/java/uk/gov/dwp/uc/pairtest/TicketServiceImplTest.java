package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImplTest {

    private TicketService ticketService;
    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;

    private final static long ACCOUNT_ID = 123L;
    private final static long INVALID_ACCOUNT_ID = 0L;

    @BeforeEach
    public void setup() {
        ticketPaymentService = Mockito.mock(TicketPaymentService.class);
        seatReservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    public void testIfValidTicketPurchaseIsSuccessful() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
       
        ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, childTickets, infantTickets);

        verify(ticketPaymentService,times(1)).makePayment(ACCOUNT_ID, 40);
        verify(seatReservationService,times(1)).reserveSeat(ACCOUNT_ID, 2);
    }

    @Test
    public void testIfAdultOnlyTicketPurchaseIsSuccessful() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
       
        ticketService.purchaseTickets(ACCOUNT_ID, adultTickets);

        verify(ticketPaymentService,times(1)).makePayment(ACCOUNT_ID, 50);
        verify(seatReservationService,times(1)).reserveSeat(ACCOUNT_ID, 2);
    }

    @Test
    public void testIfAdultAndChildOnlyTicketPurchaseIsSuccessful() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 4);
       
        ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, childTickets);

        verify(ticketPaymentService,times(1)).makePayment(ACCOUNT_ID, 110);
        verify(seatReservationService,times(1)).reserveSeat(ACCOUNT_ID, 6);
    }

    @Test
    public void testIfAdultAndInfantOnlyTicketPurchaseIsSuccessful() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 6);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);
       
        ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, infantTickets);

        verify(ticketPaymentService,times(1)).makePayment(ACCOUNT_ID, 150);
        verify(seatReservationService,times(1)).reserveSeat(ACCOUNT_ID, 6);
    }

    @Test
    public void testIfValidTicketPurchaseWithInvalidAccountIdThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
       
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(INVALID_ACCOUNT_ID, adultTickets, childTickets, infantTickets));

        assertTrue(exception.getMessage().contains("Account ID 0 is invalid."));
    }

    @Test
    public void testINegativeTicketRequestThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -10);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
       
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, childTickets, infantTickets));

        assertTrue(exception.getMessage().contains("Number of tickets requested cannot be less than 0. Current number of tickets requested: -10"));
    }

    @Test
    public void testIfMaxTicketPurchaseExceededThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 50);
       
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, childTickets, infantTickets));

        assertTrue(exception.getMessage().contains("58 tickets attempted to be purchased, more than 25 tickets cannot be purchased in a single session."));
    }

    @Test
    public void testIfChildAndInfantOnlyTicketPurchaseThrowsException() {
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, childTickets, infantTickets));

        assertTrue(exception.getMessage().contains("There are no Adult tickets in this booking. At least ONE Adult ticket MUST be purchased when purchasing Child and Infant tickets."));
    }

    @Test
    public void testIfChildOnlyTicketPurchaseThrowsException() {
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, childTickets));

        assertTrue(exception.getMessage().contains("There are no Adult tickets in this booking. At least ONE Adult ticket MUST be purchased when purchasing Child and Infant tickets."));
    }

    @Test
    public void testIfInfantOnlyTicketPurchaseThrowsException() {
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, infantTickets));

        assertTrue(exception.getMessage().contains("There are no Adult tickets in this booking. At least ONE Adult ticket MUST be purchased when purchasing Child and Infant tickets."));
    }

    @Test
    public void testIfMultipleInfantAndSingleAdultTicketPurchaseThrowsException() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, 
            () -> ticketService.purchaseTickets(ACCOUNT_ID, adultTickets, childTickets, infantTickets));

        assertTrue(exception.getMessage().contains("1 Adult ticket(s) and 3 Infant ticket(s) attempted to be purchased. There needs to be at least ONE Adult ticket booked for EACH Infant ticket per booking."));
    }
}
