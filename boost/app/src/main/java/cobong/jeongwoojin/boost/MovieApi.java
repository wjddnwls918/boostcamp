package cobong.jeongwoojin.boost;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MovieApi {
    public MyApi provideMovie() {
        return new Retrofit.Builder()
                //통신할 서버의 주소
                .baseUrl("https://openapi.naver.com")
                //네트워크 요청 로그를 표시
                .client(provideOkHttpClient(provideLoggingIntercepter()))
                //받은 응답을 옵저버블 형태로 변환
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())

                .build()

                .create(MyApi.class);

    }

    //이 클라이언트를 통해 오고 가는 네트워크 요청/응답을 로그로 표시하도록 합니다.
    private OkHttpClient provideOkHttpClient(HttpLoggingInterceptor interceptor) {
        return  new OkHttpClient.Builder().addInterceptor(interceptor).build();
    }

    //네트워크 요청/응답을 로그에 표시하는 Intercepter 객체를 생성.
    private HttpLoggingInterceptor provideLoggingIntercepter() {
         HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
         interceptor.setLevel( HttpLoggingInterceptor.Level.BODY);

         return interceptor;
    }
}
