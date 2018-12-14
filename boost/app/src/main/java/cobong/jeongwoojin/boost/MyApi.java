package cobong.jeongwoojin.boost;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

interface MyApi {

    String clientID = "dwrqrBuieaKSlxA6gqS1";
    String clientSecret = "NYz5k048Gn";

    @Headers({"X-Naver-Client-Id: "+clientID,"X-Naver-Client-Secret: "+clientSecret})
    @GET("/v1/search/movie.json")
    Observable<MovieResult> getMovie(@Query("query") String query, @Query("display") Integer display);
}
