package ucb.edu.bo.sumajflow.dto.socio;

import java.util.List;

/**
 * DTO para respuesta paginada de socios
 */
public class SociosPaginadosDto {

    private List<SocioResponseDto> socios;
    private Integer paginaActual;
    private Integer totalPaginas;
    private Long totalElementos;
    private Integer elementosPorPagina;

    // Estadísticas adicionales
    private Long totalAprobados;
    private Long totalPendientes;
    private Long totalRechazados;

    public SociosPaginadosDto() {
    }

    public SociosPaginadosDto(List<SocioResponseDto> socios, Integer paginaActual,
                              Integer totalPaginas, Long totalElementos, Integer elementosPorPagina) {
        this.socios = socios;
        this.paginaActual = paginaActual;
        this.totalPaginas = totalPaginas;
        this.totalElementos = totalElementos;
        this.elementosPorPagina = elementosPorPagina;
    }

    // Getters y Setters
    public List<SocioResponseDto> getSocios() {
        return socios;
    }

    public void setSocios(List<SocioResponseDto> socios) {
        this.socios = socios;
    }

    public Integer getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(Integer paginaActual) {
        this.paginaActual = paginaActual;
    }

    public Integer getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(Integer totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public Long getTotalElementos() {
        return totalElementos;
    }

    public void setTotalElementos(Long totalElementos) {
        this.totalElementos = totalElementos;
    }

    public Integer getElementosPorPagina() {
        return elementosPorPagina;
    }

    public void setElementosPorPagina(Integer elementosPorPagina) {
        this.elementosPorPagina = elementosPorPagina;
    }

    public Long getTotalAprobados() {
        return totalAprobados;
    }

    public void setTotalAprobados(Long totalAprobados) {
        this.totalAprobados = totalAprobados;
    }

    public Long getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(Long totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public Long getTotalRechazados() {
        return totalRechazados;
    }

    public void setTotalRechazados(Long totalRechazados) {
        this.totalRechazados = totalRechazados;
    }
}