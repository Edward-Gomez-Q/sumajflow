package ucb.edu.bo.sumajflow.dto.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OsrmResponse {
    private String code;
    private List<Route> routes;

    @Data
    public static class Route {
        private Double distance; // en metros
        private Double duration; // en segundos
        private String geometry;
    }
}