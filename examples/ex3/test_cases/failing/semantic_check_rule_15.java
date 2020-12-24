class Main {
    public static void main(String[] args) {
        System.out.println(1);
    }
}

class A { }

class B extends A {
    int theVar;

    public int foo() {
        return 0;
    }
}

class C extends A {
    int theVar;

    public int foo() {
        return 1;
    }
}

class D extends C {
    public int bar(int anotherVar) {
        int[] max;
        int theVar;

        max = new int[theVar * anotherVar]

        if (anotherVar < theVar) {
            max[anotherVar] = theVar;
        } else {
            max[theVar] = anotherVar;
        }

        return max[theVar * anotherVar];
    }
}
