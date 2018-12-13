package cobong.jeongwoojin.boost;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Movie;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import cobong.jeongwoojin.boost.databinding.ActivityMainBinding;
import cobong.jeongwoojin.boost.databinding.ViewItemBinding;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //RecyclerView
    static List<MovieItem> infiniteList;
    static int curPnt;
    LinearLayoutManager linearLayoutManager;
    MyAdapter myAdapter;
    ActivityMainBinding binding;

    //영화 정보
    MovieResultJson movieResultJson;
    static RequestQueue queue;
    GetMovieInfo getMovieInfo;
    String clientID = "dwrqrBuieaKSlxA6gqS1";
    String clientSecret = "NYz5k048Gn";
    String apiAdd = "https://openapi.naver.com/v1/search/movie.json";

    //검색
    static String input;
    InputMethodManager imm;
    Boolean isScrolling = false;
    int currentItems, totalItems, scrollOutItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        //검색 입력시
        binding.searchMovie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //네트워크 상태
                if(isNetwork()) {
                    //입력창 내리기
                    imm.hideSoftInputFromWindow(binding.inputMovie.getWindowToken(), 0);

                    infiniteList = new ArrayList<>();
                    curPnt = 0;

                    //입력값 저장
                    input = binding.inputMovie.getText().toString();
                    if(input.equals("")){
                        Toast.makeText(getApplicationContext(),"검색어를 입력해주세요",Toast.LENGTH_SHORT).show();
                    }else {
                        //영화정보 얻어오기
                        getMovieInfo = new GetMovieInfo();
                        getMovieInfo.execute();
                    }

                } else {
                   Toast.makeText(MainActivity.this,"네트워크 연결상태를 확인하세요.",Toast.LENGTH_LONG).show();
                }

            }
        });


    }

    //네트워크 연결상태 확인
    private Boolean isNetwork() {
       ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
       boolean isMobileAvailable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable();
       boolean isMobileConnect = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
       boolean isWifiAvailable = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable();
       boolean isWifiConnect = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();

       if((isWifiAvailable&&isWifiConnect) || (isMobileAvailable && isMobileConnect)) {
           return true;
       }else {
           return false;
       }
    }



    //어뎁터
    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<MovieItem> list;

        public MyAdapter(List<MovieItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //item match
            ViewItemBinding viewItemBinding = ViewItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(viewItemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {

            //이미지 저장
            if(!movieResultJson.items.get(position).image.equals("")) {
                Picasso.get()
                        .load(movieResultJson.items.get(position).image)
                        .resize(100, 170)
                        .error(R.drawable.questionmark)
                        .into(holder.viewItemBinding.thumb);
            }else {
                holder.viewItemBinding.thumb.setImageResource(R.drawable.questionmark);
            }

            //view_item 정보 저장
            holder.viewItemBinding.movietitle.setText(Html.fromHtml(movieResultJson.items.get(position).title));
            holder.viewItemBinding.rating.setRating((Float.parseFloat(movieResultJson.items.get(position).userRating)/2.0f));
            holder.viewItemBinding.year.setText(movieResultJson.items.get(position).pubDate);
            holder.viewItemBinding.director.setText(movieResultJson.items.get(position).director);
            holder.viewItemBinding.actor.setText(movieResultJson.items.get(position).actor);

            //영화 정보 링크 연결
            holder.viewItemBinding.movieInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(movieResultJson.items.get(position).link));
                    startActivity(intent);
                }
            });

        }

        //뷰홀더
        class MyViewHolder extends RecyclerView.ViewHolder {

            ViewItemBinding viewItemBinding;

            public MyViewHolder(ViewItemBinding binding) {
                super(binding.getRoot());
                this.viewItemBinding = binding;
            }

        }


        public void setMovies(ArrayList<MovieItem> movies) {
            list.addAll(movies);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }


    //Volley 사용해서 데이터 얻어오기
    public class GetMovieInfo extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            queue = Volley.newRequestQueue(getApplicationContext());

            String url = apiAdd+"?query="+ input+"&display=100";

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    //요청 성공시
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //리스트
                            Log.d("movieInfo",response);
                            //Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                            //refreshList(response);

                            processResponse(response);

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("movieInfoGetError", "[" + error.getMessage() + "]");
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<>();
                    params.put("X-Naver-Client-Id", clientID);
                    params.put("X-Naver-Client-Secret", clientSecret);

                    return params;
                }
            };

            queue.add(request);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    //list 삽입
    public void processResponse(String response){

            Gson gson = new Gson();
            movieResultJson = gson.fromJson(response,MovieResultJson.class);

            if(movieResultJson.items.size()==0) {
                Toast.makeText(getApplicationContext(),"'"+input+"' 검색 결과는 없습니다..",Toast.LENGTH_LONG).show();
            }else {
                if(movieResultJson.items.size() < 10){
                    for(int i = 0; i< movieResultJson.items.size(); i++){
                        infiniteList.add(movieResultJson.items.get(i));
                    }
                    curPnt += movieResultJson.items.size();
                }else {
                    for(int i=0; i<10; i++){
                        infiniteList.add(movieResultJson.items.get(i));
                    }
                    curPnt += 10;
                }

                linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                binding.mainRecycler.setLayoutManager(linearLayoutManager);
                myAdapter = new MyAdapter(infiniteList);
                binding.mainRecycler.setAdapter(myAdapter);

                //스크롤 10개단위로 끊어서
                binding.mainRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {

                            isScrolling = true;
                        }

                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        currentItems = linearLayoutManager.getChildCount();
                        totalItems = linearLayoutManager.getItemCount();
                        scrollOutItems = linearLayoutManager.findFirstVisibleItemPosition();

                        if(isScrolling && (currentItems + scrollOutItems == totalItems) ) {

                            isScrolling = false;
                            fetchData();

                        }

                    }
                });
            }

    }

    //RecyclerView 10개 단위로 불러오기
    private void fetchData() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if(curPnt >= movieResultJson.items.size()) {

                }else {

                    if(curPnt + 10 >= movieResultJson.items.size()){
                        for(int i = curPnt; i< movieResultJson.items.size(); i++){
                            infiniteList.add(movieResultJson.items.get(i));
                        }
                        curPnt += (movieResultJson.items.size()-curPnt);
                    }else {
                        for(int i=0; i<10; i++){
                            infiniteList.add(movieResultJson.items.get(curPnt+i));
                        }
                        curPnt += 10;
                    }

                    myAdapter.notifyDataSetChanged();
                }

            }
        }, 100);
    }
}
