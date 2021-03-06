/** 
 * Simplified version from CSE 593, University of Washington.
 *
 * A Distributed System in Java using Web Services.
 * 
 * Failures should be reported via the return value.  For example, 
 * if an operation fails, you should return either false (boolean), 
 * or some error code like -1 (int).
 *
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

package server.ws;

import server.InvalidTransactionException;
import server.TransactionAbortedException;

import javax.jws.WebService;
import javax.jws.WebMethod;


@WebService
public interface ResourceManager {

    // Transaction operations //
    @WebMethod
    public void start(int id);

    @WebMethod
    public void doCommit(int id);

    @WebMethod
    public void doAbort(int id);

    @WebMethod
    public void prepare(int id) throws TransactionAbortedException, InvalidTransactionException;

    @WebMethod
    public void selfDestruct();

    // Flight operations //
    
    /* Add seats to a flight.  
     * In general, this will be used to create a new flight, but it should be 
     * possible to add seats to an existing flight.  Adding to an existing 
     * flight should overwrite the current price of the available seats.
     *
     * @return success.
     */
    @WebMethod
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice); 

    /**
     * Delete the entire flight.
     * This implies deletion of this flight and all its seats.  If there is a 
     * reservation on the flight, then the flight cannot be deleted.
     *
     * @return success.
     */   
    @WebMethod
    public boolean deleteFlight(int id, int flightNumber); 

    /* Return the number of empty seats in this flight. */
    @WebMethod
    public int queryFlight(int id, int flightNumber); 

    /* Return the price of a seat on this flight. */
    @WebMethod
    public int queryFlightPrice(int id, int flightNumber); 


    // Car operations //

    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    @WebMethod
    public boolean addCars(int id, String location, int numCars, int carPrice); 
    
    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */		    
    @WebMethod
    public boolean deleteCars(int id, String location); 

    /* Return the number of cars available at this location. */
    @WebMethod
    public int queryCars(int id, String location); 

    /* Return the price of a car at this location. */
    @WebMethod
    public int queryCarsPrice(int id, String location); 


    // Room operations //
    
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    @WebMethod
    public boolean addRooms(int id, String location, int numRooms, int roomPrice); 			    

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */
    @WebMethod
    public boolean deleteRooms(int id, String location); 

    /* Return the number of rooms available at this location. */
    @WebMethod
    public int queryRooms(int id, String location); 

    /* Return the price of a room at this location. */
    @WebMethod
    public int queryRoomsPrice(int id, String location); 

    /* Reserve a seat on this flight. */
    @WebMethod
    public boolean reserveFlight(int id, int customerId, int flightNumber); 

    /* Reserve a car at this location. */
    @WebMethod
    public boolean reserveCar(int id, int customerId, String location); 

    /* Reserve a room at this location. */
    @WebMethod
    public boolean reserveRoom(int id, int customerId, String location);

    @WebMethod
    public void unreserveItem(int id, String key, String location, int count);

    @WebMethod
    public boolean shutdown();
}
