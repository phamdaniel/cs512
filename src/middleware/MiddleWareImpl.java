package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
import server.*;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.*;
import java.net.URL;

@WebService(endpointInterface = "middleware.ws.MiddleWare")
public class MiddleWareImpl implements middleware.ws.MiddleWare {

    middleware.ResourceManagerImplService rm;
    middleware.ResourceManager[] proxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    private TransactionManager tm = new TransactionManager(this);

    public MiddleWareImpl() {
//        String hosts[] = {"142.157.165.20","142.157.165.20","142.157.165.113","142.157.165.113" };
        String hosts[] = {"localhost","localhost","localhost" };
        int[] ports = {4000,4001,4002};

        setupProxies(hosts, ports);
    }

    public MiddleWareImpl(String[] hosts, int[] ports) {
        if (ports.length != hosts.length) {
            System.out.println("Ports array length doesn't match hosts array length");
            return;
        }

        setupProxies(hosts, ports);
    }

    public void setupProxies(String[] hosts, int[] ports)
    {
        proxy = new middleware.ResourceManager[ports.length];

        try {
            for (int i = 0; i < ports.length; i++) {
                URL wsdlLocation = new URL("http://" + hosts[i] + ":" + ports[i] + "/rm/service?wsdl");
                rm = new middleware.ResourceManagerImplService(wsdlLocation);
                proxy[i] = rm.getResourceManagerImplPort();

                System.out.println("Connection established with " + hosts[i] + ":" + ports[i]);
            }
        } catch (MalformedURLException e) {
            System.out.println(e);
        }
    }


    protected middleware.ResourceManager getCarProxy() { return proxy[MiddleWareImpl.CAR_PROXY_INDEX]; }
    protected middleware.ResourceManager getFlightProxy() { return proxy[MiddleWareImpl.FLIGHT_PROXY_INDEX]; }
    protected middleware.ResourceManager getRoomProxy() { return proxy[MiddleWareImpl.ROOM_PROXY_INDEX]; }

    // CUSTOMER

    protected RMHashtable customerHT = new RMHashtable();

    private Map<Integer, Map<String, RMItem>> readSet = new HashMap<>();
    private Map<Integer, Map<String, RMItem>> writeSet = new HashMap<>();

    // Read a data item.
    private RMItem readData(int transactionID, String key) {
        synchronized(customerHT) {
            // Check the writeSet
            if (writeSet.get(transactionID).containsKey(key))
                return writeSet.get(transactionID).get(key);

            // Check the readSet
            if (readSet.get(transactionID).containsKey(key))
                return readSet.get(transactionID).get(key);

            // Else get data from database
            RMItem item = (RMItem) customerHT.get(key);
            this.readSet.get(transactionID).put(key, item);
            return item;
        }
    }

    // Write a data item.
    private void writeData(int transactionID, String key, RMItem newValue, boolean commit) {
        synchronized(customerHT) {
            if (commit) {
                customerHT.put(key, newValue);
            } else {
                // save the data in the write set.
                this.writeSet.get(transactionID).put(key, newValue);
            }
        }
    }

    // Remove the item out of storage.
    protected void removeData(int transactionID, String key, boolean commit) {
        synchronized(customerHT) {
            if (commit) {
                customerHT.remove(key);
            } else {
                // remove from the write set.
                // tag it with null to mark for deletion
                this.writeSet.get(transactionID).put(key, null);
            }
        }
    }

    // CUSTOMER TRANSACTIONS HELPERS
    public void startCustomer(int transactionID) {
        if (writeSet.containsKey(transactionID)) return;

        writeSet.put(transactionID, new HashMap<String, RMItem>());
        readSet.put(transactionID, new HashMap<String, RMItem>());
    }

    public void commitCustomer(int transactionID) {
        if (!writeSet.containsKey(transactionID)) return;

        for (Map.Entry<String, RMItem> entry : writeSet.get(transactionID).entrySet()) {
            if (entry.getValue() == null) {
                this.removeData(transactionID, entry.getKey(), true);
            } else {
                this.writeData(transactionID, entry.getKey(), entry.getValue(), true);
            }
        }

        this.writeSet.remove(transactionID);
        this.readSet.remove(transactionID);
    }

    public void abortCustomer(int transactionID) {
        if (!writeSet.containsKey(transactionID)) return;
        this.writeSet.remove(transactionID);
        this.readSet.remove(transactionID);
    }

    // TRANSACTIONS
    @Override
    public int start() {
        return this.tm.start();
    }

    @Override
    public boolean commit(int id) {
        return this.tm.commit(id);
    }

    @Override
    public boolean abort(int id) {
        return this.tm.abort(id);
    }

    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;

        return getFlightProxy().addFlight(id, flightNumber, numSeats, flightPrice);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;

        return getFlightProxy().deleteFlight(id, flightNumber);
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 1))) return 0;

        return getFlightProxy().queryFlight(id, flightNumber);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 1))) return 0;

        return getFlightProxy().queryFlightPrice(id, flightNumber);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;

        return getCarProxy().addCars(id, location, numCars, carPrice);
    }

    @Override
    public boolean deleteCars(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;

        return getCarProxy().deleteCars(id, location);
    }

    @Override
    public int queryCars(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 1, 1))) return 0;

        return getCarProxy().queryCars(id, location);
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 1, 1))) return 0;

        return getCarProxy().queryCarsPrice(id, location);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;

        return getRoomProxy().addRooms(id, location, numRooms, roomPrice);
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;

        return getRoomProxy().deleteRooms(id, location);
    }

    @Override
    public int queryRooms(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 2, 1))) return 0;

        return getRoomProxy().queryRooms(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        if (!this.tm.addOperation(id, new Operation(location, 2, 1))) return 0;

        return getRoomProxy().queryRoomsPrice(id, location);
    }

    // Customer operations //

    @Override
    public int newCustomer(int id) {
        Trace.info("INFO: RM::newCustomer(" + id + ") called.");
        // Generate a globally unique Id for the new customer.
        int customerId = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(customerId);
        writeData(id, cust.getKey(), cust, false);
        Trace.info("RM::newCustomer(" + id + ") OK: " + customerId);

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return 0;

        return customerId;
    }

    // This method makes testing easier.
    @Override
    public boolean newCustomerId(int id, int customerId) {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") called.");

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            cust = new Customer(customerId);
            writeData(id, cust.getKey(), cust, false);
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " +
                    customerId + ") failed: customer already exists.");
            return false;
        }
    }

    // Delete customer from the database.
    @Override
    public boolean deleteCustomer(int id, int customerId) {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") called.");

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return false;
        } else {

            // Remove the customer from the storage.
            removeData(id, cust.getKey(), false);
            Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        }
    }

    private boolean customerExists(int id, int customerId) {
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        return (cust != null);
    }

    public void setCustomerReservation(int id, int customerId, String key, String location, int price) {
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        cust.reserve(key, location, price);
    }

    // Return data structure containing customer reservation info.
    // Returns null if the customer doesn't exist.
    // Returns empty RMHashtable if customer exists but has no reservations.
    public Object[] getCustomerReservations(int id, int customerId) {
        Trace.info("RM::getCustomerReservations(" + id + ", "
                + customerId + ") called.");

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 1))) return null;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.info("RM::getCustomerReservations(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return null;
        } else {
            Collection<Object> col = new ArrayList<Object>();
            col.addAll(cust.getReservations().keySet());

            Collection<ReservedItem> ris = cust.getReservations().values();
            for (ReservedItem ri : ris) {
                col.add(ri.getCount());
            }

            return col.toArray();
        }
    }

    // Return a bill.
    @Override
    public String queryCustomerInfo(int id, int customerId) {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + ") called.");

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 1))) return null;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            // Returning an empty bill means that the customer doesn't exist.
            return "";
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + "): \n");
            System.out.println(s);
            return s;
        }
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: customer doesn't exist.");
            return false;
        }
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;
        boolean reserved = getFlightProxy().reserveFlight(id, customerId, flightNumber);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;
            return false;
        }

        int price = queryFlightPrice(id, flightNumber);
        setCustomerReservation(id, customerId, Flight.getKey(flightNumber), String.valueOf(flightNumber), price);

        return true;
    }

    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;
        boolean reserved = getCarProxy().reserveCar(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;
            return false;
        }

        int price = queryCarsPrice(id, location);
        setCustomerReservation(id, customerId, Car.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        boolean reserved = getRoomProxy().reserveRoom(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;
            return false;
        }

        int price = queryRoomsPrice(id, location);
        setCustomerReservation(id, customerId, Room.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        // Assuming everything has to work for reserve itinerary to return true
        boolean result = false;

        for (String number : flightNumbers) {
            result = reserveFlight(id, customerId, Integer.parseInt(number));
            if (result) {
                if (!this.tm.addOperation(id, new Operation(Integer.toString(number), 0, 2))) return false;
            }
        }

        if (car) {
            result = reserveCar(id, customerId, location);
            if (result) {
                if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;
            }
        }

        if (room) {
            result = reserveRoom(id, customerId, location);
            if (result) {
                if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;
            }
        }

        return result;
    }

    @Override
    public boolean shutdown() {
        // Check that no transaction are running.
        if (tm.isActive()) return false;

        // Shutdown all the RMs
        try {
            this.getFlightProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }
        try {
            this.getCarProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }
        try {
            this.getRoomProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }

        // Shutdown the middleware
        System.exit(0);

        return true;
    }
}