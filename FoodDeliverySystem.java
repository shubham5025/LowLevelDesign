import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class MenuItem{
    String name;
    double price;

    public MenuItem(String name,double price)
    {
        this.name=name;
        this.price=price;
    }
    public double getPrice() {
        return price;
    }

    public String getName(){
        return this.name;
    }
}

class Restaurant{
    String name;
    double rating;
    int capacity;
    AtomicInteger activeOrder = new AtomicInteger(0);
    ConcurrentHashMap<String,MenuItem> menu;

    public Restaurant(String name, int capacity, double rating){
        this.name=name;
        this.capacity=capacity;
        this.rating=rating;
        this.menu= new ConcurrentHashMap<>();
    }

    public void addMenuItems(MenuItem item)
    {
        this.menu.put(item.getName(), item);
    }

    public void updateMenuItem(String name, double price)
    {
        MenuItem item=menu.get(name);
        item.price=price;
        this.menu.put(name,item);
    }
    public double getRating() {
        return rating;
    }

    public boolean checkCapacity(){
        int currentOrder=activeOrder.get();
        if(currentOrder<this.capacity) {
            activeOrder.compareAndSet(currentOrder, currentOrder + 1);
            return true;
        }
        return false;
    }

    public void completeOrder()
    {
        int currentOrder=activeOrder.get();
        if(currentOrder<=0)
            throw new IllegalStateException("No Active Order");
        activeOrder.decrementAndGet();
    }

}

class RestaurantReposiotry{
    Map<String, Restaurant> restaurants;

    public RestaurantReposiotry(){
        restaurants=new HashMap<>();
    }

    public void addRestaurant(Restaurant restaurant)
    {
        if(restaurants.putIfAbsent(restaurant.name, restaurant)!=null)
            throw new IllegalStateException("Restaurant Already there");
    }
    public Restaurant getRestaurant(String name)
    {
        return restaurants.get(name);
    }
    public List<Restaurant> getAllRestaurant()
    {
        List<Restaurant> restaurantList= new ArrayList<>();
        for(Map.Entry<String, Restaurant> res:restaurants.entrySet())
        {
            restaurantList.add(res.getValue());
        }
        return restaurantList;
    }

    public void placeOrder(Restaurant r, Order order)
    {
        if(r!=null && r.checkCapacity()){
            order.setAssignRestaurant(r);
            order.setStatus(OrderStatus.Accepted);
        }
        else{
            order.setStatus(OrderStatus.Rejected);
            System.out.println("Order Can not be assigned to Restaurant");
        }
    }
}
enum OrderStatus
{
    Pending,
    Accepted,
    Completed,
    Rejected

}

@Data
class Order{
    int id;
    OrderStatus status;
    Restaurant assignRestaurant;

    // itemId -> qty
    Map<String, Integer> items;

    public Order(int id, Map<String, Integer> items)
    {
        this.id=id;
        this.items=items;
        this.assignRestaurant=null;
        this.status=OrderStatus.Pending;
    }

    public double totalPrice(Restaurant restaurant)
    {
        double sum=0;
        for(Map.Entry<String,Integer> item: this.items.entrySet())
        {
            MenuItem mi= restaurant.menu.get(item.getKey());
            if(mi==null)
                throw new IllegalStateException("Menu Item not found in restaurant");
            sum+=mi.getPrice()*item.getValue();
        }
        return sum;
    }



    public Restaurant getAssignRestaurant() {
        return assignRestaurant;
    }


    public void addItems(String name, Integer qty)
    {
        this.items.put(name,qty);
    }
}

interface SelectionStrategy{
    Restaurant select(List<Restaurant> candidates, Order order);
}

class LowestCostStrategy implements SelectionStrategy{

    @Override
    public Restaurant select(List<Restaurant> candidates, Order order) {
        Map<String, Integer> items=order.getItems();

        double price=0;
        double bestPrice=Double.POSITIVE_INFINITY;
        Restaurant bestRestaurant=null;
        for(Restaurant r: candidates)
        {
            try {
                if(r.checkCapacity()) {
                    price = order.totalPrice(r);
                    if(bestPrice>price)
                    {
                        bestPrice=price;
                        bestRestaurant=r;
                    }
                }
            }
            catch (IllegalStateException ex)
            {
                System.out.println("Restaurant "+ r.name + "does not have all items in order");
            }

        }
        return bestRestaurant;
    }
}

class TopRatingStrategy implements SelectionStrategy{

    @Override
    public Restaurant select(List<Restaurant> candidates, Order order) {
        Restaurant bestRestaurant=null;
        double bestRating=-1, rating=0;
        for(Restaurant r:candidates)
        {
            try{
                double cost=order.totalPrice(r);
                rating=r.getRating();
                if(rating>bestRating && r.checkCapacity())
                {
                    bestRating=rating;
                    bestRestaurant=r;
                }
            }
            catch (IllegalStateException ex){
                System.out.println("Restaurant "+ r.name + "does not have all items in order");
            }
        }
        return bestRestaurant;
    }
}
public class FoodDeliverySystem {

    public static void main(String[] args){

        Restaurant r1=new Restaurant("R1", 5, 4.5);
        r1.addMenuItems(new MenuItem("Veg Biryani", 100));
        r1.addMenuItems(new MenuItem("Paneer Butter Masala", 150));

        Restaurant r2=new Restaurant("R2", 5, 4);
        r2.addMenuItems(new MenuItem("Idli", 10));
        r2.addMenuItems(new MenuItem("Paneer Butter Masala", 175));
        r2.addMenuItems(new MenuItem("Dosa", 50));
        r2.addMenuItems(new MenuItem("Veg Biryani", 80));

        Restaurant r3=new Restaurant("R3", 1, 4.9);
        r3.addMenuItems(new MenuItem("Idli", 15));
        r3.addMenuItems(new MenuItem("Paneer Butter Masala", 175));
        r3.addMenuItems(new MenuItem("Dosa", 30));
        r3.addMenuItems(new MenuItem("Veg Manchurian", 150));

        r1.addMenuItems(new MenuItem("Chicken65", 200));
        r2.updateMenuItem("Paneer Butter Masala", 150);
        RestaurantReposiotry restaurantReposiotry=new RestaurantReposiotry();
        restaurantReposiotry.addRestaurant(r1);
        restaurantReposiotry.addRestaurant(r2);
        restaurantReposiotry.addRestaurant(r3);
        Map<String, Integer> o1= new HashMap<>();
        o1.put("Idli",3);
        o1.put("Dosa",1);
        Order order=new Order(1,o1);
        SelectionStrategy selectionStrategy=new LowestCostStrategy();
        Restaurant restaurant=selectionStrategy.select(restaurantReposiotry.getAllRestaurant(),order);
        System.out.println("Total Price of r2 "+ order.totalPrice(r2));
        System.out.println("Total Price of r3 "+ order.totalPrice(r3));
        System.out.println("Best Restaurant is "+restaurant.name+ "\n");

        restaurantReposiotry.placeOrder(restaurant,order);

        Order order2= new Order(2,o1);

        Restaurant restaurant1=selectionStrategy.select(restaurantReposiotry.getAllRestaurant(), order2);
        System.out.println("Best Restaurant is "+restaurant1.name+ "\n");

        restaurantReposiotry.placeOrder(restaurant1,order);

        Map<String, Integer> o2= new HashMap<>();
        o2.put("Veg Biryani",3);
        Order order3=new Order(3,o2);

        SelectionStrategy selectionStrategy1= new TopRatingStrategy();
        Restaurant restaurant3=selectionStrategy1.select(restaurantReposiotry.getAllRestaurant(), order3);
        System.out.println("Best Restaurant is "+restaurant3.name+ "\n");

        restaurantReposiotry.placeOrder(restaurant3,order);
        r3.completeOrder();
        Restaurant restaurant4=selectionStrategy.select(restaurantReposiotry.getAllRestaurant(), order);
        System.out.println("Best Restaurant is "+restaurant4.name+ "\n");


        Map<String, Integer> o3= new HashMap<>();
        o3.put("Paneer Tikka",3);
        Order order4=new Order(3,o3);
        Restaurant restaurant5=selectionStrategy.select(restaurantReposiotry.getAllRestaurant(), order4);
        System.out.println("New Best Restaurant is "+restaurant5.name);

    }



}
