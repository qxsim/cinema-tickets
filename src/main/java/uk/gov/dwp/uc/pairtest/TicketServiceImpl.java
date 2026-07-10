package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;

    public static final int ADULT_TICKET_PRICE = 25;
    public static final int CHILD_TICKET_PRICE = 15;
    public static final int MAX_TICKETS = 25;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        int numberOfAdultTickets = 0;
        int numberOfChildTickets = 0;
        int numberOfInfantTickets = 0;
        int totalPrice = 0;

        if (isAccountIdInvalid(accountId)) {
            throw new InvalidPurchaseException("Account ID " + accountId + " is invalid.");
        }

        for (TicketTypeRequest currentRequest : ticketTypeRequests) {
            int currentNumberOfTickets = currentRequest.getNoOfTickets();

            if (areNumberOfTicketsLessThanZero(currentNumberOfTickets)) {
                throw new InvalidPurchaseException("Number of tickets requested cannot be less than 0. Current number of tickets requested: " + currentNumberOfTickets);
            }

            switch(currentRequest.getTicketType()) {
                case ADULT:
                    numberOfAdultTickets += currentNumberOfTickets;
                    totalPrice += (currentRequest.getNoOfTickets() * ADULT_TICKET_PRICE);
                    break;
                case CHILD:
                    numberOfChildTickets += currentNumberOfTickets;
                    totalPrice += (currentRequest.getNoOfTickets() * CHILD_TICKET_PRICE);
                    break;
                case INFANT:
                    numberOfInfantTickets += currentNumberOfTickets;
                    //total price doesn't need to be updated for infant tickets because they are free.
                    break;
            }
        }

        int totalNumberOfTickets = numberOfAdultTickets + numberOfChildTickets + numberOfInfantTickets;
        int totalNumberOfBookableSeats = totalNumberOfTickets - numberOfInfantTickets;

        if (doesTicketTotalExceedMaxAmount(totalNumberOfTickets)) {
            throw new InvalidPurchaseException(totalNumberOfTickets + " tickets attempted to be purchased, more than " + MAX_TICKETS + " tickets cannot be purchased in a single session.");
        } else if (isAdultTicketNotPresentWithOtherTicketTypePurchases(numberOfAdultTickets, totalNumberOfTickets)) {
            throw new InvalidPurchaseException("There are no Adult tickets in this booking. At least ONE Adult ticket MUST be purchased when purchasing Child and Infant tickets.");
        } else if (doesInfantTicketAmountExceedAdultTicketAmount(numberOfAdultTickets, numberOfInfantTickets)) {
            throw new InvalidPurchaseException(numberOfAdultTickets + " Adult ticket(s) and " + numberOfInfantTickets + " Infant ticket(s) attempted to be purchased. There needs to be at least ONE Adult ticket booked for EACH Infant ticket per booking.");
        }

        ticketPaymentService.makePayment(accountId, totalPrice);
        seatReservationService.reserveSeat(accountId, totalNumberOfBookableSeats);
    }

    private Boolean areNumberOfTicketsLessThanZero(int numberOfTickets) {
        return numberOfTickets < 0;
    }

    private Boolean doesTicketTotalExceedMaxAmount(int ticketTotal) {
        return ticketTotal > MAX_TICKETS;
    }

    private Boolean isAccountIdInvalid(long accountId) {
        return accountId < 1;
    }

    private Boolean isAdultTicketNotPresentWithOtherTicketTypePurchases(int numberOfAdultTickets, int totalNumberOfTickets) {
        return totalNumberOfTickets > 0 && numberOfAdultTickets < 1;
    }

    private Boolean doesInfantTicketAmountExceedAdultTicketAmount(int numberOfAdultTickets, int numberOfInfantTickets) {
        return numberOfInfantTickets > numberOfAdultTickets;
    }
}
