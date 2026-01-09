import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

interface NotificationObserver{
    void onMeetingScheduled(Meeting meeting);
}

@Data
@AllArgsConstructor
class MeetingUser implements NotificationObserver{

    String name;
    @Override
    public void onMeetingScheduled(Meeting meeting) {
        System.out.println("Meeting Created by "+meeting.getOrganizer()+ " Start Time "+meeting.getStartTime()
        +" Ends At "+meeting.getEndTime());
    }
}

@Data
class Meeting{
    static int counter=0;
    int id;
    int meetingRoomAssigned;
    LocalDateTime startTime;
    LocalDateTime endTime;
    MeetingUser organizer;
    List<MeetingUser> participants;
    Meeting(LocalDateTime startTime, LocalDateTime endTime, List<MeetingUser> participants, MeetingUser organizer, int meetingRoomAssigned)
    {
        counter++;
        this.id=counter;
        this.participants=participants;
        this.meetingRoomAssigned=meetingRoomAssigned;
        this.organizer=organizer;
        this.startTime=startTime;
        this.endTime=endTime;
    }
    Meeting(LocalDateTime startTime, LocalDateTime endTime)
    {
        this.startTime=startTime;
        this.endTime=endTime;
    }

}

@Data
class MeetingRoom{
    static int counter=0;
    int id;
    int capacity;
    private TreeSet<Meeting> meetings;
    MeetingRoom(int capacity)
    {
        this.id=counter;
        counter++;
        this.capacity=capacity;
        meetings = new TreeSet<>((m1,m2)->m1.getStartTime().compareTo(m2.getStartTime()));
    }

    void addMeeting(Meeting meeting)
    {
        this.meetings.add(meeting);
    }
    boolean isAvailable(LocalDateTime startTime, LocalDateTime endTime, int capacity)
    {
        if(this.getCapacity()<capacity)
            return false;
        Meeting meeting=new Meeting(startTime,endTime);
        Meeting before = meetings.floor(meeting);
        if(before!=null && before.getEndTime().isAfter(startTime))
            return false;
        Meeting after=meetings.ceiling(meeting);
        if(after!=null && after.getStartTime().isBefore(endTime))
            return false;

        return true;
    }

    void showAllMeetings()
    {
        meetings.forEach(meeting -> System.out.println("Meeting start time " +meeting.getStartTime()
        +" Meeting End Time "+meeting.getEndTime()+" and Organiser "+meeting.getOrganizer().getName()));
    }


}
class MeetingSchedulerService{
    TreeSet<MeetingRoom> meetingRooms;

    static class MeetingSchedulerHelper{
        public final static MeetingSchedulerService meetingSchedulerService=new MeetingSchedulerService();
    }
    static MeetingSchedulerService getInstance()
    {
        return MeetingSchedulerHelper.meetingSchedulerService;
    }
    MeetingSchedulerService()
    {
        this.meetingRooms=new TreeSet<>((m1,m2)->
        {
            if(m1.getCapacity()!=m2.getCapacity())
                return Integer.compare(m1.getCapacity(), m2.getCapacity());
            return Integer.compare(m1.getId(),m2.getId());
        });
    }
    void addMeetingRoom(MeetingRoom meetingRoom)
    {
        this.meetingRooms.add(meetingRoom);
    }
    String schedule(LocalDateTime startTime, LocalDateTime endTime, MeetingUser organizer,
                    ArrayList<MeetingUser> participants)
    {
        participants.add(organizer);
        MeetingRoom assignedMeetingRoom=null;
        for(MeetingRoom meetingRoom:meetingRooms.tailSet(new MeetingRoom(participants.size())))
        {
            if(meetingRoom.isAvailable(startTime,endTime,participants.size()))
            {
                assignedMeetingRoom=meetingRoom;
                break;
            }
        }
        if(assignedMeetingRoom!=null)
        {
            Meeting meeting=new Meeting(startTime,endTime,participants,organizer,assignedMeetingRoom.getId());
            assignedMeetingRoom.addMeeting(meeting);

            for(MeetingUser meetingUser:participants)
                meetingUser.onMeetingScheduled(meeting);

            return "Meeting Assigned to Meeting room "+assignedMeetingRoom.getId();
        }
        return "No Meeting Room Available";
    }

}
public class MeetingScheduler {
    public static void main(String[] args)
    {
        MeetingSchedulerService meetingSchedulerService=MeetingSchedulerService.getInstance();
        for(int i=0;i<2;i++)
        {
            MeetingRoom meetingRoom=new MeetingRoom(4-i);
            meetingSchedulerService.addMeetingRoom(meetingRoom);
        }
        MeetingUser meetingUser1=new MeetingUser("Shubham");
        MeetingUser meetingUser2=new MeetingUser("Jhalani");
        MeetingUser meetingUser3=new MeetingUser("Kirty");
        MeetingUser meetingUser4=new MeetingUser("Astha");
        MeetingUser meetingUser5=new MeetingUser("Vishnu");

        String meeting =meetingSchedulerService.schedule(LocalDateTime.of(2025,12,18,10,0),
                LocalDateTime.of(2025,12,18,12,0),meetingUser1,new ArrayList<>(List.of(meetingUser4)));
        String meeting1 =meetingSchedulerService.schedule(LocalDateTime.of(2025,12,18,11,59),
                LocalDateTime.of(2025,12,18,13,30),meetingUser2,new ArrayList<>(List.of(meetingUser4,meetingUser3)));
        String meeting2=meetingSchedulerService.schedule(LocalDateTime.of(2025,12,18,13,29),
                LocalDateTime.of(2025,12,18,13,31),meetingUser1,new ArrayList<>(List.of(meetingUser3,meetingUser2,meetingUser5)));
        System.out.println(meeting);
        System.out.println(meeting1);
        System.out.println(meeting2);

        for(MeetingRoom meetingRoom: meetingSchedulerService.meetingRooms)
        {
            System.out.println(meetingRoom.getId());
            meetingRoom.showAllMeetings();
        }
    }
}
