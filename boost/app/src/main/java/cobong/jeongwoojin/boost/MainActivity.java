package cobong.jeongwoojin.boost;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.Uri;
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

import com.squareup.picasso.Picasso;

import cobong.jeongwoojin.boost.databinding.ActivityMainBinding;
import cobong.jeongwoojin.boost.databinding.ViewItemBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    //RecyclerView
    static ArrayList<MovieItem> result;
    static ArrayList<MovieItem> infiniteList;
    static int curPnt;
    LinearLayoutManager linearLayoutManager;
    MyAdapter myAdapter;
    ActivityMainBinding binding;

    //검색
    static String input;
    InputMethodManager imm;
    Boolean isScrolling = false;
    int currentItems, totalItems, scrollOutItems;

    //
    MyApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        api = new MovieApi().provideMovie();

        //검색 입력시
        binding.searchMovie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //네트워크 상태
                if(isNetwork()) {
                    //입력창 내리기
                    imm.hideSoftInputFromWindow(binding.inputMovie.getWindowToken(), 0);

                    result = new ArrayList<>();
                    infiniteList = new ArrayList<>();
                    curPnt = 0;

                    //입력값 저장
                    input = binding.inputMovie.getText().toString();
                    if(input.equals("")){
                        Toast.makeText(getApplicationContext(),"검색어를 입력해주세요",Toast.LENGTH_SHORT).show();
                    }else {

                        //영화정보 얻어오기
                        Observable<MovieResult> observable = api.getMovie(input,100);
                        new CompositeDisposable().add(
                                observable.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(
                                        (response) -> {
                                            return Observable.just(response.items);
                                        }
                                )
                                .subscribe( (data)-> {
                                    Log.d("data test",data.toString());
                                    result = data;
                                    processResponse();
                                }, (e) -> {e.printStackTrace();}
                                ));
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
            if(!result.get(position).image.equals("")) {
                Picasso.get()
                        .load(result.get(position).image)
                        .resize(100, 170)
                        .error(R.drawable.questionmark)
                        .into(holder.viewItemBinding.thumb);
            }else {
                holder.viewItemBinding.thumb.setImageResource(R.drawable.questionmark);
            }

            //view_item 정보 저장
            holder.viewItemBinding.movietitle.setText(Html.fromHtml(result.get(position).title));
            holder.viewItemBinding.rating.setRating((Float.parseFloat(result.get(position).userRating)/2.0f));
            holder.viewItemBinding.year.setText(result.get(position).pubDate);
            holder.viewItemBinding.director.setText(result.get(position).director);
            holder.viewItemBinding.actor.setText(result.get(position).actor);

            //영화 정보 링크 연결
            holder.viewItemBinding.movieInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.get(position).link));
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

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    //list 삽입
    public void processResponse(){

            if(result.size()==0) {
                Toast.makeText(getApplicationContext(),"'"+input+"' 검색 결과는 없습니다..",Toast.LENGTH_LONG).show();
            }else {
                if(result.size() < 10){
                    for(int i = 0; i< result.size(); i++){
                        infiniteList.add(result.get(i));
                    }
                    curPnt += result.size();
                }else {
                    for(int i=0; i<10; i++){
                        infiniteList.add(result.get(i));
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

                if(curPnt >= result.size()) {

                }else {

                    if(curPnt + 10 >= result.size()){
                        for(int i = curPnt; i< result.size(); i++){
                            infiniteList.add(result.get(i));
                        }
                        curPnt += (result.size()-curPnt);
                    }else {
                        for(int i=0; i<10; i++){
                            infiniteList.add(result.get(curPnt+i));
                        }
                        curPnt += 10;
                    }

                    myAdapter.notifyDataSetChanged();
                }

            }
        }, 100);
    }
}
