package hospital;

/** Simple patient model. */
public class Patient {
    private final int id;
    private final String name;
    private final int age;
    private final String room;

    public Patient(int id, String name, int age, String room) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.room = room;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getRoom() { return room; }

    @Override
    public String toString() { return "#" + id + " " + name; }
}
