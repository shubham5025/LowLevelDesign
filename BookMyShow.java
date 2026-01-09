import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
class Person{
    String id;
    String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class City{
    String cityId;
    String cityName;
    String State;
}

@Data
@AllArgsConstructor
class Movie{
    String name;
    long durationInMins;
}

@Data
@AllArgsConstructor
class Theater{
    String theaterId;
    List<Screen> screens;
    City city;
}

@Data
@AllArgsConstructor
class Screen{
    String screenId;
    Theater theater;
    List<Seat> seats;
    List<Show> shows;
}

enum  SeatType{
    SILVER,
    GOLD,
    PLATINUM
}

@Data
@AllArgsConstructor
class Seat{
    String seatId;
    SeatType seatType;
    double price;
}
enum SeatStatus{
    VACANT,
    RESERVED,
    BOOKED
}
@Data
@AllArgsConstructor
class ShowSeat{
    Seat seat;
    SeatStatus seatStatus;
}
@Data
@AllArgsConstructor
class Show{
    String showID;
    List<ShowSeat> showSeats;
    Movie movie;
    Screen screen;
    LocalDate localDate;
    Time starTime;
    Time endTime;
}

@Data
@AllArgsConstructor
class Booking{
    int bookingId;
    Show show;
    List<ShowSeat> showSeatsAlloted;
    double price;
    PaymentPayStatus paymentPayStatus;
}

class MovieCatalogue{
    Map<City, List<Movie>> movieCityMap;
    List<Movie> allMovies;

    List<Movie> searchMovieByCity(String cityName)
    {
       return movieCityMap.entrySet().stream().
                filter(entry->entry.getKey().getCityName().equals(cityName))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    void addMovies(Movie movie, City city)
    {
        List<Movie> movies= movieCityMap.computeIfAbsent(city,k->new ArrayList<>());
        movies.add(movie);
        movieCityMap.put(city,movies);
    }

}

class TheaterCatalogue{
    Map<Movie, List<Theater>> movieTheaterMap;
    List<Theater> allTheater;

    List<Theater> getALlMovieTheater(Movie movie)
    {
        return movieTheaterMap.get(movie);
    }

}

class BookMyShowManager{
    MovieCatalogue movieCatalogue;
    TheaterCatalogue theaterCatalogue;

    Movie addMovie(String name, long durationInMins, City city)
    {
        Movie movie=new Movie(name,durationInMins);
        movieCatalogue.addMovies(movie,city);
        return movie;
    }

}
public class BookMyShow {
    public static void main(String[] args)
    {

    }
}
