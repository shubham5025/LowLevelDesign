import java.util.ArrayList;
import java.util.List;

enum Direction{
    UP,
    DOWN,
    IDLE
}
enum DoorAction{
    OPEN,
    CLOSE
}
interface Button{
    boolean isPressed();
    boolean press();
}

class HallButton implements Button{
    boolean status;
    Direction direction;
    public HallButton(boolean status, Direction direction)
    {
        this.status=status;
        this.direction=direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public boolean isPressed() {
        return status;
    }

    @Override
    public boolean press() {
        status=!status;
        return status;
    }
}

class ElevatorButton implements Button{

    int floorNumber;
    boolean status;

    ElevatorButton(int floorNumber, boolean status)
    {
        this.floorNumber=floorNumber;
        this.status=status;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public boolean isPressed() {
        return status;
    }

    @Override
    public boolean press() {
        status=!status;
        return status;
    }
}
class DoorButton implements Button{
    boolean status;
    DoorAction doorAction;
    DoorButton(DoorAction doorAction, boolean status)
    {
        this.doorAction=doorAction;
        this.status=status;
    }

    @Override
    public boolean isPressed() {
        return false;
    }

    @Override
    public boolean press() {
        return false;
    }
}
interface Panel{

}
class InsidePanel implements  Panel{
    List<ElevatorButton> elevatorButtons;
    List<DoorButton> doorButtons;
    InsidePanel(int floors)
    {
        elevatorButtons=new ArrayList<>();
        doorButtons=new ArrayList<>();
        for(int i = 0; i<=floors; i++)
        {
            elevatorButtons.add(new ElevatorButton(i,false));
        }
        for(int i=0;i<2;i++)
        {
            doorButtons.add(new DoorButton(DoorAction.values()[i],false));
        }
    }
    public boolean pressElevatorButton(int floorId)
    {
        return this.elevatorButtons.get(floorId).press();
    }

    public boolean pressDoorButton(int doorNumber)
    {
        return this.doorButtons.get(doorNumber).press();
    }
}
class OutsidePanel implements Panel{
    List<HallButton> hallButtons;
    OutsidePanel()
    {
        hallButtons=new ArrayList<>();
        for(int i=0;i<2;i++)
        {
            hallButtons.add(new HallButton(false,Direction.values()[i]));
        }
    }
    public boolean pressHallButton(int id)
    {
        return this.hallButtons.get(id).press();
    }
}
class Floor{
    int floorId;
    OutsidePanel outsidePanel;
    Floor(int floorId, OutsidePanel outsidePanel)
    {
        this.floorId=floorId;
        this.outsidePanel=outsidePanel;
    }
}
class Building{
    List<Floor> floorList;
    Building(int numOfFloors) {
        floorList = new ArrayList<>();
        OutsidePanel outsidePanel = new OutsidePanel();
        for (int i = 0; i <= numOfFloors; i++) {
            floorList.add(new Floor(i, outsidePanel));
        }
    }
}
class Elevator{
    int elevatorId;
    InsidePanel insidePanel;
    Direction direction;
    int floorId;
    DoorAction doorAction;
    List<Integer> requests;
    Elevator(int elevatorId, InsidePanel insidePanel, int floorId)
    {
        this.elevatorId=elevatorId;
        this.insidePanel=insidePanel;
        this.floorId=floorId;
        this.direction=Direction.IDLE;
        this.doorAction=DoorAction.CLOSE;
        requests=new ArrayList<>();
    }
    void move(int de)
    {

    }
    void move(int de, Direction direction)
    {

    }

}
class ElevatorManager{
    Building building;
    static List<Elevator>elevators;
    ElevatorManager(int numOfFloors, int numOfElevator)
    {
        elevators=new ArrayList<>();
        building=new Building(numOfFloors);
        InsidePanel insidePanel=new InsidePanel(numOfFloors);
        for(int i=0;i<=numOfElevator;i++)
        {
            elevators.add(new Elevator(i,insidePanel,0));
        }
    }

    int findNearestElevator(int floor, Direction direction)
    {
        int bestElevator=-1;
        int bestScore=10000000,score;
        for(int i=1;i<elevators.size();i++)
        {
            score=getElevatorScore(elevators.get(i), floor, direction);
            if(score<bestScore)
            {
                bestScore=score;
                bestElevator=i;
            }
        }
        return bestElevator;
    }

    int getElevatorScore(Elevator elevator, int floor, Direction direction)
    {
        int score=Math.abs(floor-elevator.floorId);
        if(elevator.direction==Direction.IDLE)
            return score;

        if(direction==elevator.direction)
        {
            if((elevator.direction==Direction.UP && floor>=elevator.floorId)||
                    (elevator.direction==Direction.DOWN && floor<elevator.floorId))
                return score;
            else return 1000+score;
        }
        return 2000+score;
    }

    int callNearestLift(int destFloor, Direction direction)
    {
        int nearestElevator=findNearestElevator(destFloor,direction);
        elevators.get(nearestElevator).move(destFloor);
        System.out.println("Elevator Reached at Floor "+destFloor);
        return nearestElevator;
    }
}
public class ElevatorDesign {
    public static void main(String[] args)
    {
        ElevatorManager elevatorManager=new ElevatorManager(15,5);
        int nearestLift =elevatorManager.callNearestLift(5,Direction.DOWN);
        ElevatorManager.elevators.get(nearestLift).move(2,Direction.DOWN);

        ElevatorManager.elevators.get(1).floorId=2;
        ElevatorManager.elevators.get(1).direction=Direction.UP;

        nearestLift = elevatorManager.callNearestLift(4,Direction.UP);
        int nearestLift1=elevatorManager.callNearestLift(6,Direction.UP);
        System.out.println(nearestLift+ " "+nearestLift1);
        ElevatorManager.elevators.get(nearestLift1).move(6,Direction.UP);
        nearestLift=elevatorManager.callNearestLift(8, Direction.DOWN);
        System.out.println(nearestLift);

        ElevatorManager.elevators.get(1).floorId=2;
        ElevatorManager.elevators.get(1).direction=Direction.IDLE;
        ElevatorManager.elevators.get(2).floorId=9;
        ElevatorManager.elevators.get(2).direction=Direction.UP;
        ElevatorManager.elevators.get(3).floorId=15;
        ElevatorManager.elevators.get(3).direction=Direction.DOWN;

        nearestLift=elevatorManager.callNearestLift(10,Direction.UP);
        System.out.println(nearestLift);
    }

}
