class Main {
    public static void main(String[] args) {
        System.out.println(1);
    }
}

class A {}

class B extends A {
    public int theMethod() {
        return 1;
    }

    public int anotherMethod() {
        return this.theMethod()
    }
}

class C extends A {
    public int theMethod() {
        return 1;
    }
}

class D extends C {
    public int anotherMethod(B b) {
        int[] array;
        int value;

        array = new int[this.theMethod() + 1];

        if (this.theMethod() > b.theMethod()) {
            array[this.theMethod()] = this.theMethod() - b.theMethod();
        } else {
            while (this.theMethod() < b.theMethod())
            value = this.theMethod() * b.theMethod();
        }



    }
}
