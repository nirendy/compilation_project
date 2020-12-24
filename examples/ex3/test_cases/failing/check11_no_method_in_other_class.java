class Main {
    public static void main(String[] a) { 
        System.out.println(3);
    }
}

class A {

    public int otherFunc() {
        return 0;
    }
}

class Simple {
    public int func() {
        A bar;
        int num;

        bar = new A();
        num = bar.runFunc();

        return 0;
    }
}