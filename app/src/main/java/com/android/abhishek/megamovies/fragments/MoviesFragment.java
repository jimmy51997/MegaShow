package com.android.abhishek.megamovies.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.abhishek.megamovies.R;
import com.android.abhishek.megamovies.adapter.ListAdapter;
import com.android.abhishek.megamovies.model.ListResults;
import com.android.abhishek.megamovies.model.ShowList;
import com.android.abhishek.megamovies.viewModel.MovieListVM;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MoviesFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    //  XML Views
    @BindView(R.id.fragmentRvAtMv) RecyclerView recyclerView;
    @BindView(R.id.fragmentPbAtMv) ProgressBar progressBar;
    @BindView(R.id.errorLayoutAtMv) RelativeLayout errorLayout;
    @BindView(R.id.showPageNo) TextView pageText;
    @BindView(R.id.nextAtMv) Button next;
    @BindView(R.id.previousAtMv) Button previous;
    private ImageView menu;

    //  Api Key
    @BindString(R.string.apiKey) String API_KEY;

    //  Sort Features
    @BindString(R.string.popularQ) String POPULAR;
    @BindString(R.string.topRatedQ) String TOP_RATED;
    @BindString(R.string.upcomingQ) String UPCOMING;
    @BindString(R.string.nowPlayingQ) String NOW_PLAYING;

    // Temporary Variable To Store Sort Pref
    @BindString(R.string.popularQ) String MOVIE_SORT_BY;

    //  Key To Restore From Shared Pref
    @BindString(R.string.movieSortPref) String movieSortKey;
    @BindString(R.string.currentPage) String currentPageKey;
    @BindString(R.string.recyclerViewState) String recyclerStateKey;

    //  Temporary Variables
    private int NO_OF_IMAGE = 2;
    private int currentPage = 1;
    private int previousPage = 1;
    private int totalPage = 1;
    private List<ListResults> listResults;
    private ShowList tempShowList = new ShowList();
    private Toast toast;
    private boolean flag = true;
    private Parcelable parcelable;

    public MoviesFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies, container, false);
        ButterKnife.bind(this,view);
        loadPreferences();

        if(savedInstanceState != null){
            if(savedInstanceState.containsKey(movieSortKey)){
                MOVIE_SORT_BY = savedInstanceState.getString(movieSortKey);
            }
            if(savedInstanceState.containsKey(currentPageKey)){
                currentPage = savedInstanceState.getInt(currentPageKey);
                previousPage = currentPage;
            }
            if(savedInstanceState.containsKey(recyclerStateKey)){
                 parcelable = ((Bundle) savedInstanceState).getParcelable(recyclerStateKey);
            }
            flag = false;
        }

        menu = getActivity().findViewById(R.id.menuBtn);
        registerForContextMenu(menu);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(movieSortKey,MOVIE_SORT_BY);
        outState.putInt(currentPageKey, currentPage);
        outState.putParcelable(recyclerStateKey,recyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        checkOrientation();
        loadMovies();

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(toast != null){
                    toast.cancel();
                }
                toast = Toast.makeText(getActivity(),getResources().getString(R.string.pressLong),Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    private void checkOrientation(){
        int configuration = this.getResources().getConfiguration().orientation;
        if(configuration == Configuration.ORIENTATION_PORTRAIT){
            NO_OF_IMAGE = 2;
        }else{
            NO_OF_IMAGE = 4;
        }
    }

    private void loadPreferences(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        MOVIE_SORT_BY = sharedPreferences.getString(movieSortKey,POPULAR);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void loadMovies(){
        if(!networkStatus()&&flag){
            showError();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        if (MOVIE_SORT_BY.equals(POPULAR)) {
            MovieListVM moviesViewModel = ViewModelProviders.of(this).get(MovieListVM.class);
            moviesViewModel.getPopularMoviesList(API_KEY, currentPage,previousPage).observe(this, new Observer<ShowList>() {
                @Override
                public void onChanged(@Nullable ShowList showList) {
                    if(tempShowList.equals(showList)){
                        return;
                    }else{
                        tempShowList = showList;
                        setMovieList(showList);
                    }

                }
            });
        } else if(MOVIE_SORT_BY.equals(TOP_RATED)){
            MovieListVM moviesViewModel = ViewModelProviders.of(this).get(MovieListVM.class);
            moviesViewModel.getTopRatedMoviesList(API_KEY, currentPage,previousPage).observe(this, new Observer<ShowList>() {
                @Override
                public void onChanged(@Nullable ShowList showList) {
                    if(tempShowList.equals(showList)){
                        return;
                    }else{
                        tempShowList = showList;
                        setMovieList(showList);
                    }
                }
            });
        }else if(MOVIE_SORT_BY.equals(UPCOMING)){
            MovieListVM moviesViewModel = ViewModelProviders.of(this).get(MovieListVM.class);
            moviesViewModel.getUpcomingMoviesList(API_KEY, currentPage,previousPage).observe(this, new Observer<ShowList>() {
                @Override
                public void onChanged(@Nullable ShowList showList) {
                    if(tempShowList.equals(showList)){
                        return;
                    }else{
                        tempShowList = showList;
                        setMovieList(showList);
                    }
                }
            });
        }else {
            MovieListVM moviesViewModel = ViewModelProviders.of(this).get(MovieListVM.class);
            moviesViewModel.getNowPlayingMoviesList(API_KEY, currentPage,previousPage).observe(this, new Observer<ShowList>() {
                @Override
                public void onChanged(@Nullable ShowList showList) {
                    if(tempShowList.equals(showList)){
                        return;
                    }else{
                        tempShowList = showList;
                        setMovieList(showList);
                    }
                }
            });
        }
    }

    private void setMovieList(ShowList showList){
        if(showList == null || showList.getResults().size() == 0){
            showError();
            return;
        }
        progressBar.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        totalPage = showList.getTotalPages();
        listResults = showList.getResults();
        previousPage = currentPage;

        doPagination();

        int resId = R.anim.recycler_animation;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getActivity(), resId);
        recyclerView.setLayoutAnimation(animation);

        ListAdapter movieListAdapter = new ListAdapter(listResults,getActivity(),"Movie");
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), NO_OF_IMAGE));
        recyclerView.setAdapter(movieListAdapter);
        if(parcelable != null){
            recyclerView.getLayoutManager().onRestoreInstanceState(parcelable);
        }

    }

    private void showError(){
        progressBar.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        pageText.setVisibility(View.INVISIBLE);
        next.setVisibility(View.INVISIBLE);
        previous.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
    }

    private boolean networkStatus(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isAvailable() && connectivityManager.getActiveNetworkInfo().isConnected());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals(movieSortKey)){
            MOVIE_SORT_BY = sharedPreferences.getString(movieSortKey,POPULAR);
            loadMovies();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.movie_sort_by,menu);

        MenuItem popular = menu.findItem(R.id.popularMv);
        MenuItem topRated = menu.findItem(R.id.mostRatedMv);
        MenuItem upcoming = menu.findItem(R.id.upcomingMv);
        MenuItem nowPlaying = menu.findItem(R.id.nowPlayMv);

        if(MOVIE_SORT_BY.equalsIgnoreCase(POPULAR )){
            popular.setChecked(true);
        }else if(MOVIE_SORT_BY.equalsIgnoreCase(NOW_PLAYING)){
            nowPlaying.setChecked(true);
        }else if(MOVIE_SORT_BY.equalsIgnoreCase(UPCOMING)){
            upcoming.setChecked(true);
        }else{
            topRated.setChecked(true);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.popularMv :
                item.setChecked(true);
                MOVIE_SORT_BY = POPULAR;
                loadMovies();
                return true;
            case R.id.mostRatedMv :
                item.setChecked(true);
                MOVIE_SORT_BY = TOP_RATED;
                loadMovies();
                return true;
            case R.id.upcomingMv :
                item.setChecked(true);
                MOVIE_SORT_BY = UPCOMING;
                loadMovies();
                return true;
            case R.id.nowPlayMv :
                item.setChecked(true);
                MOVIE_SORT_BY = NOW_PLAYING;
                loadMovies();
                return true;

        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @OnClick(R.id.retryAtMv)
    void refresh(){
        loadMovies();
    }
    @OnClick(R.id.nextAtMv)
    void loadNext(){
        currentPage++;
        flag = true;
        loadMovies();
    }
    @OnClick(R.id.previousAtMv)
    void loadPrv(){
        currentPage--;
        flag = true;
        loadMovies();
    }

    private void doPagination(){
        if(currentPage == 1){
            previous.setVisibility(View.GONE);
        }else if(currentPage >1){
            previous.setVisibility(View.VISIBLE);
        }

        if(currentPage >= totalPage){
            next.setVisibility(View.GONE);
        }else if(currentPage < totalPage){
            next.setVisibility(View.VISIBLE);
        }
        pageText.setVisibility(View.VISIBLE);
        pageText.setText("Page "+ currentPage +" of "+ totalPage);
    }

}
