package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 16.2.2016.
 */
enum Direction {
    FORWARD("forward"), REVERSE("reverse"), BOTH("both");

    private String direction;

    Direction(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return this.direction;
    }

    public boolean is(Direction that){
        return this.equals(that);
    }
}
