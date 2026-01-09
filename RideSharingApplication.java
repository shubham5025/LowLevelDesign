import lombok.Data;

import java.beans.VetoableChangeListenerProxy;
import java.util.*;

enum Gender{
    MALE,
    FEMALE
}
@Data
class Passenger{
    String userId;
    String name;
    Gender gender;
    int age;
    int offered;
    int taken;
    List<Vechile> vechiles;

    //For Ride Handling or lifecycle management we can have DriverProfile Class with driver information and ref here
    // Lifecycle will be managed in Ride Service
    Passenger(String name, int age, Gender gender)
    {
        this.age=age;
        this.gender=gender;
        this.name=name;
        this.offered=0;
        this.taken=0;
    }
}

enum VechileType{
    SEDAN,
    SUV,
    Economy,
    ACTIVA,
    BALENO,
    POLO,
    SWIFT
}
enum VechileState{
    RIDING,
    IDLE
}
@Data
class Vechile{
    String id;
    VechileType vechileType;
    String userId;
    int capacity;
    VechileState vechileState;
    Vechile(String id, VechileType vechileType, String userId, int capacity)
    {
        this.id=id;
        this.capacity=capacity;
        this.vechileState=VechileState.IDLE;
        this.vechileType=vechileType;
        this.userId=userId;
    }

    void setVehileState(VechileState vechileState)
    {
        this.vechileState=vechileState;
    }
}
@Data
class Place{
    String city;
    Place(String city)
    {
        this.city=city;
    }

}
@Data
class Ride{
    String rideId;
    String vechileId;
    String userID;
    String origin;
    String destination;
    int availableSeat;
    VechileType vechileType;
    List<Passenger> passengerList;
    Ride(String rideId, String vechileId, String userID, String origin, String destination, int availableSeat, VechileType vechileType)
    {
        this.rideId=rideId;
        this.vechileId=vechileId;
        this.userID=userID;
        this.origin=origin;
        this.destination=destination;
        this.availableSeat=availableSeat;
        this.passengerList=new ArrayList<>();
        this.vechileType=vechileType;
    }
}

class PassengerService{
     Map<String, Passenger> passengers;
     Map<String, Vechile> vechiles;

    PassengerService()
    {
        passengers=new HashMap<>();
        vechiles=new HashMap<>();
    }
    Passenger addPassenger(String name, int age, Gender gender)
    {
        Passenger passenger=new Passenger(name, age, gender);
        this.passengers.putIfAbsent(name,passenger);
        return passenger;
    }

    Vechile addVechile(String id, VechileType vechileType, String userId, int capacity)
    {
        Vechile vechile=new Vechile(id,vechileType,userId,capacity);
        vechiles.putIfAbsent(id,vechile);
        return vechile;
    }
    void incrementOffered(String userId)
    {
        Passenger passenger=passengers.get(userId);
        passenger.setOffered(passenger.getOffered()+1);
    }
    void incrementTaken(String userId)
    {
        Passenger passenger=passengers.get(userId);
        passenger.setTaken(passenger.getTaken()+1);
    }
    Vechile getVechile(String vechileID)
    {
        return this.vechiles.get(vechileID);
    }
    Passenger getPassenger(String passengerId)
    {
        Passenger passenger=passengers.get(passengerId);
        if(passenger==null)
            throw new IllegalStateException("No Passenger Exist");
        return passenger;
    }

    void printRideStatus()
    {
        passengers.forEach((s, passenger) ->
                System.out.println("Passenger "+passenger.getName()+" Offered="+passenger.getOffered()+" Taken="+passenger.getTaken()));
    }
}

enum SearchingStrategyType{
    MOST_VACANT,
    PreferredVechile
}

class SearchRideStrategyFactory{
    private static final Map<SearchingStrategyType, SearchRideStrategy> searchRideStrategy=new HashMap<>();

    static {
        searchRideStrategy.put(SearchingStrategyType.PreferredVechile, new PreferredVechileSearchRideStrategy());
        searchRideStrategy.put(SearchingStrategyType.MOST_VACANT, new MostVacantSearchRideStrategy());
    }

    static SearchRideStrategy getSearchingRideStrategy(SearchingStrategyType searchingStrategyType)
    {
        return searchRideStrategy.getOrDefault(searchingStrategyType,
                searchRideStrategy.get(SearchingStrategyType.MOST_VACANT));
    }
}
interface SearchRideStrategy{
    List<Ride> searchRide(String origin, String destination, int capacity, List<Ride> rides, VechileType vechileType);
}

class MostVacantSearchRideStrategy implements SearchRideStrategy{

    @Override
    public List<Ride> searchRide(String origin, String destination, int capacity, List<Ride> rides, VechileType vechileType) {

        List<Ride> selectedRide=new ArrayList<>();
        Ride ride=rides.stream().filter(r-> r.getOrigin().equals(origin) && r.getDestination().equals(destination)).max(Comparator.comparingInt(Ride::getAvailableSeat)).orElse(null);
        if(ride!=null)
            selectedRide.add(ride);
        return selectedRide;
    }
}

class PreferredVechileSearchRideStrategy implements SearchRideStrategy{

    @Override
    public List<Ride> searchRide(String origin, String destination, int capacity, List<Ride> rides, VechileType vechileType) {

        List<Ride> selectedRides;
        selectedRides=rides.stream().filter(ride-> ride.getOrigin().equals(origin) &&
                ride.getDestination().equals(destination)&&ride.getVechileType()==vechileType).toList();
        return selectedRides;
    }
}

class RideService{
    Map<String, Ride> activeRide;
    PassengerService passengerService;
    RideService(PassengerService passengerService)
    {
        this.activeRide=new HashMap<>();
        this.passengerService=passengerService;
    }
    boolean validateRide(String vechileId)
    {
        Vechile vechile=passengerService.getVechile(vechileId);
        return vechile != null && vechile.getVechileState() == VechileState.IDLE;
    }

    Ride createRide(String rideId, String vechileId, String userID, String origin, String destination, int availableSeat, VechileType vechileType)
    {
        if(!validateRide(vechileId))
            return null;

        Ride ride= new Ride(rideId, vechileId, userID, origin, destination, availableSeat, vechileType);
        passengerService.incrementOffered(userID);
        passengerService.getVechile(vechileId).setVechileState(VechileState.RIDING);
        activeRide.put(rideId,ride);
        return ride;
    }

    List<Ride> searchRide(String origin, String destination, int capacity, SearchRideStrategy searchRideStrategy, VechileType vechileType)
    {
        List<Ride> rides=activeRide.values().stream().toList();
        return searchRideStrategy.searchRide(origin,destination,capacity,rides,vechileType);
    }

    Ride selectRide(Ride ride, String userId)
    {
        Passenger passenger=passengerService.getPassenger(userId);
        ride.passengerList.add(passenger);
        return ride;
    }

    void endRide(Ride ride)
    {
        if(!activeRide.containsKey(ride.getRideId()))
        {
            System.out.println("Ride Does not exist");
            throw new IllegalStateException("Ride Does not exist");
        }
        Ride endRide=activeRide.remove(ride.getRideId());
        passengerService.getVechile(ride.getVechileId()).setVechileState(VechileState.IDLE);
        for(Passenger passenger:ride.getPassengerList())
            passengerService.incrementTaken(passenger.getUserId());
        System.out.println("Ride Ended Successfully, Ride "+ride.getRideId());
    }
}
class RideSharingSystem{
    PassengerService passengerService;
    RideService rideService;
    SearchRideStrategy searchRideStrategy;
    SearchRideStrategyFactory searchRideStrategyFactory;
    private static class RideSharingSystemHelper{
        public final static RideSharingSystem rideSharingSystem=new RideSharingSystem();
    }
    static RideSharingSystem getInstance()
    {
        return RideSharingSystemHelper.rideSharingSystem;
    }
    private RideSharingSystem()
    {
        this.passengerService=new PassengerService();
        this.rideService=new RideService(this.passengerService);
    }

    Passenger addPassenger(String name, int age, Gender gender)
    {
        return passengerService.addPassenger(name, age, gender);
    }

    Vechile addVechile(String id, VechileType vechileType, String userId, int capacity)
    {
        return passengerService.addVechile(id, vechileType, userId, capacity);
    }

    Ride offerRide(String rideId, String vechileId, String userID, String origin, String destination,
                   int availableSeat, VechileType vechileType)
    {
        Ride ride=rideService.createRide(rideId, vechileId, userID, origin, destination, availableSeat, vechileType);
        if(ride==null) {
            System.out.println("This Ride Can not be created, Vechile Already in a Ride");
            return ride;
        }
        return ride;
    }

    void selectRide(String userId, String origin, String destination, int capacity,
                     VechileType vechileType, SearchingStrategyType searchingStrategyType)
    {
        searchRideStrategy=SearchRideStrategyFactory.getSearchingRideStrategy(searchingStrategyType);
        List<Ride> rides=rideService.searchRide(origin, destination, capacity, searchRideStrategy, vechileType);
        System.out.println(rides.isEmpty());
        if(rides.isEmpty()) {
            System.out.println("No Rides Found with existing specification");
            return;
        }
        System.out.println("Selected Ride "+rides.getFirst());
        Ride ride=rideService.selectRide(rides.getFirst(),userId);
    }

    void printRideStats()
    {
        passengerService.printRideStatus();
    }
}
public class RideSharingApplication {
    public static void main(String[] args)
    {
        RideSharingSystem rideSharingSystem=RideSharingSystem.getInstance();
        rideSharingSystem.addPassenger("Rohan", 36, Gender.MALE);
        rideSharingSystem.addVechile("KA-01-12345", VechileType.SWIFT,"Rohan",4);

        rideSharingSystem.addPassenger("Shashank", 29, Gender.MALE);
        rideSharingSystem.addVechile("TS-05-62395", VechileType.BALENO, "Shashank",4);

        rideSharingSystem.addPassenger("Nandini", 29, Gender.FEMALE);

        rideSharingSystem.addPassenger("Shipra", 27, Gender.FEMALE);
        rideSharingSystem.addVechile("KA-05-41491", VechileType.POLO, "Shipra", 4);
        rideSharingSystem.addVechile("KA-12-12332", VechileType.ACTIVA, "Shipra", 4);

        rideSharingSystem.addPassenger("Gaurav", 29, Gender.MALE);

        rideSharingSystem.addPassenger("Rahul", 35, Gender.MALE);
        rideSharingSystem.addVechile("KA-05-1234", VechileType.SUV, "Rahul", 4);

        rideSharingSystem.offerRide("1", "KA-01-12345", "Rohan", "Hyderabad","Bangalore",1,VechileType.SWIFT);

        rideSharingSystem.offerRide("2", "KA-12-12332", "Shipra", "Bangalore","Mysore",1,VechileType.ACTIVA);

        rideSharingSystem.offerRide("3", "KA-05-41491", "Shipra", "Bangalore","Mysore",2,VechileType.POLO);

        rideSharingSystem.offerRide("4", "TS-05-62395", "Shashank", "Hyderabad","Bangalore",2,VechileType.BALENO);

        rideSharingSystem.offerRide("5", "KA-05-1234", "Rahul", "Hyderabad","Bangalore",5,VechileType.SUV);

        rideSharingSystem.offerRide("6", "KA-01-12345", "Rohan", "Bangalore","Pune",1,VechileType.SWIFT);

        rideSharingSystem.selectRide("Nandini", "Bangalore", "Mysore", 1, null,SearchingStrategyType.MOST_VACANT);

        rideSharingSystem.selectRide("Gaurav", "Bangalore", "Mysore",1,VechileType.ACTIVA,SearchingStrategyType.PreferredVechile);

        rideSharingSystem.selectRide("Shashank", "Mumbai", "Bangalore",1,null,SearchingStrategyType.MOST_VACANT);

        rideSharingSystem.selectRide("Rohan", "Hyderabad", "Bangalore", 1, VechileType.BALENO, SearchingStrategyType.PreferredVechile);
        rideSharingSystem.selectRide("Shashank", "Hyderabad","Bangalore",1, VechileType.POLO, SearchingStrategyType.PreferredVechile);
        rideSharingSystem.printRideStats();
    }
}
