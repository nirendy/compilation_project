class Main {
    public static void main(String[] a) {
        System.out.println(3);
    }
}

class A {

}

class B extends A {

}

class Simple {

    public A func() {
        B bar;

        bar = new B();

        return bar;
    }
}