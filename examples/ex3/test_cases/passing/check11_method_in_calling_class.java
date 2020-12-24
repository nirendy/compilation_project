class Main {
    public static void main(String[] a) { 
        System.out.println(3);
    }
}

class Simple {
    public int runFunc() {
        return 0;
    }

    public int func() {
        int bar;

        bar = this.runFunc();

        return 0;
    }
}