package proyecto.vetefaas.model;

public class Rol {

    private Integer id;
    private String rol;

    public Rol() {
    }

    public Rol(Integer id, String rol) {
        this.id = id;
        this.rol = rol;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
