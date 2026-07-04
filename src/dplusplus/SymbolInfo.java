package dplusplus;

public class SymbolInfo {
    public enum Category { VARIABLE, CONSTANT, OBJECT, METHOD, CLASS }
    public enum TypeKind { INTEIRO, BOOLEANO, CLASSE }

    private String id;
    private Category category;
    private TypeKind typeKind;
    private String className; // Preenchido apenas se o tipo for CLASSE

    public SymbolInfo(String id, Category category, TypeKind typeKind) {
        this.id = id;
        this.category = category;
        this.typeKind = typeKind;
    }

    public SymbolInfo(String id, Category category, String className) {
        this.id = id;
        this.category = category;
        this.typeKind = TypeKind.CLASSE;
        this.className = className;
    }

    // Getters e Setters
    public String getId() { return id; }
    public Category getCategory() { return category; }
    public TypeKind getTypeKind() { return typeKind; }
    public String getClassName() { return className; }
}