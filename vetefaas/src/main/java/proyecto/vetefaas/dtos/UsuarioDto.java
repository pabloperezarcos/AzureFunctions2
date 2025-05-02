package proyecto.vetefaas.dtos;

/**
 * DTO (Data Transfer Object) que representa los datos de un usuario.
 * 
 * Se utiliza principalmente para la comunicación entre el cliente
 * (Postman u otro)
 * y el controlador (UsuarioController). Permite transportar la información del
 * usuario
 * de forma sencilla y segura.
 */
public class UsuarioDto {
    private String nombre;
    private String email;

    // getters y setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
