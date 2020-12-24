class Main {
    public static void main(String[] a) {
        System.out.println(3);
    }
}

class Simple {

    int field;

    public int runFunc() {
        return 0;
    }

    public int func() {
        field = this.runFunc();

        field[1] = 5;

        return 0;
    }
}