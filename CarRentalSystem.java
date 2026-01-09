import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

enum MotorVehicleType{
    CAR,
    BIKE,
    TRUCK
}
enum Status{
    ACTIVE,
    INACTIVE
}
class MotorVehicle{
    int id;
    MotorVehicleType MotorVehicleType;
    int price;
    Status status;
    MotorVehicle(int id, MotorVehicleType MotorVehicleType, int price, Status status)
    {
        this.id=id;
        this.MotorVehicleType=MotorVehicleType;
        this.price=price;
        this.status=status;
    }
}

class MotorVehicleInventory{
    List<MotorVehicle> MotorVehicleList=new ArrayList<>();
    void addMotorVehicle(MotorVehicle e)
    {
        this.MotorVehicleList.add(e);
    }
}

class Location{
    String city;
    String state;
    String pinCode;
    Location(String city, String state, String pinCode)
    {
        this.city=city;
        this.state=state;
        this.pinCode=pinCode;
    }
}
class User{
    String name;
    String licenceNumber;
    User(String name, String licenceNumber)
    {
        this.name=name;
        this.licenceNumber = licenceNumber;
    }

}
enum PaymentPayStatus{
    PAID,
    UNPAID
}
class Reservation{
    User user;
    MotorVehicle veh;
    LocalDate bookingDate;
    LocalDate startDate;
    LocalDate dueDate;
    PaymentPayStatus PaymentPayStatus;
    Reservation(User user, MotorVehicle veh, LocalDate bookingDate, LocalDate startDate, LocalDate dueDate)
    {
        this.user=user;
        this.veh=veh;
        this.bookingDate=bookingDate;
        this.dueDate=dueDate;
        this.startDate=startDate;
        this.PaymentPayStatus=PaymentPayStatus.UNPAID;
    }
}
class Store{
    int storeId;
    Location location;
    MotorVehicleInventory MotorVehicleInventory;
    List<Reservation> reservationList = new ArrayList<>();
    Store(int storeId, Location location)
    {
        this.storeId=storeId;
        this.location=location;
        this.MotorVehicleInventory=new MotorVehicleInventory();
    }
    List<MotorVehicle> search(MotorVehicleType MotorVehicleType)
    {
        Optional<List<MotorVehicle>> MotorVehicle= Optional.of(this.MotorVehicleInventory.MotorVehicleList.stream()
                .filter(veh -> veh.MotorVehicleType.equals(MotorVehicleType)).collect(Collectors.toList()));
        return MotorVehicle.orElse(null);
    }

}
class Bill{
    Reservation reservation;
    boolean isPaid;
    Bill(Reservation reservation, boolean isPaid)
    {
        this.reservation=reservation;
        this.isPaid=false;
    }

    public long calculateAmount()
    {
        return ChronoUnit.DAYS.between(this.reservation.startDate,this.reservation.dueDate)*this.reservation.veh.price;
    }
}
class PaymentPay{
    Bill bill;
    PaymentPay(Bill bill)
    {
        this.bill=bill;

    }
    void pay()
    {
        System.out.println("Amount Paid "+this.bill.calculateAmount());
    }
}
public class CarRentalSystem {
    List<Store> stores;
    public static void main(String[] args)
    {
        Location location = new Location("Pune", "Maharashtra", "401123");
        MotorVehicle vehicl= new MotorVehicle(1, MotorVehicleType.CAR, 400, Status.ACTIVE);
        MotorVehicle MotorVehicle1= new MotorVehicle(2, MotorVehicleType.CAR, 200, Status.ACTIVE);
        Store store= new Store(1, location);
        store.MotorVehicleInventory=new MotorVehicleInventory();
        store.MotorVehicleInventory.addMotorVehicle(vehicl);
        store.MotorVehicleInventory.addMotorVehicle(MotorVehicle1);
        List<MotorVehicle> v=store.search(MotorVehicleType.CAR);
        System.out.println("Selected MotorVehicle "+v.get(0).id);
        System.out.println("Selected MotorVehicle "+v.get(1).id);
        Reservation reservation= new Reservation(new User("Shubham", "123"),v.get(1),
                LocalDate.of(2025,12,12), LocalDate.of(2025,12,14),
                LocalDate.of(2025,12,18));

        Bill bill=new Bill(reservation, false);
        PaymentPay PaymentPay=new PaymentPay(bill);
        PaymentPay.pay();

    }

}