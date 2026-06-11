package DesignPatterns.Composite;

public class MenuItem extends MenuComponent {
    private final String Name;
    private final String Description;
    private final Boolean isVegetarian;
    private final Double Price;

    public MenuItem(String name, String description, Boolean isVegetarian, Double price) {
        Name = name;
        Description = description;
        this.isVegetarian = isVegetarian;
        Price = price;
    }

    @Override
    public void print() {
        System.out.println("Name: " + Name
                + ", Description: " + Description
                + ", isVegetarian: " + isVegetarian
                + ", Price: " + Price);
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public String getDescription() {
        return Description;
    }

    public Boolean getVegetarian() {
        return isVegetarian;
    }

    @Override
    public double getPrice() {
        return Price;
    }
}
