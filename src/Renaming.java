import ast.Program;

public abstract class Renaming {

    Program program;
    String classOfRenamedObject;
    String methodOfRenamedObject;
    String objectName;
    String newObjectName;
    ProgramUtils utils;

    public Renaming(Program program, String classOfRenamedObject, String methodOfRenamedObject, String objectName,
                    String newObjectName) {
        this.program = program;
        this.classOfRenamedObject = classOfRenamedObject;
        this.methodOfRenamedObject = methodOfRenamedObject;
        this.objectName = objectName;
        this.newObjectName = newObjectName;
        this.utils = new ProgramUtils(program);
    }

    public abstract void rename();
}
