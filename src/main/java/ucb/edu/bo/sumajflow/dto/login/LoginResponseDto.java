package ucb.edu.bo.sumajflow.dto.login;

public class LoginResponseDto {

    private String token;
    private String refreshToken;
    private UserInfoDto user;

    // Constructors
    public LoginResponseDto() {
    }

    public LoginResponseDto(String token, String refreshToken, UserInfoDto user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserInfoDto getUser() {
        return user;
    }

    public void setUser(UserInfoDto user) {
        this.user = user;
    }
}



































