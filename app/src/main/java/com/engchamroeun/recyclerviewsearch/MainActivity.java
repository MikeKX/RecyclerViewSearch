package com.engchamroeun.recyclerviewsearch;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.engchamroeun.recyclerviewsearch.listener.OnLoadMoreListener;
import com.engchamroeun.recyclerviewsearch.model.YoutubeModel;
import com.squareup.picasso.Picasso;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private List<YoutubeModel> list = new ArrayList<>();
    private YoutubeAdapter mYoutubeAdapter;
    private SearchView search;
    private String token_page = "";
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //call view
        search = (SearchView) findViewById(R.id.search);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //call function get data from server
        getDataFromYoutubeApi();

        //search in recyclerview
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    mYoutubeAdapter.getFilter().filter("");
                    mYoutubeAdapter.notifyDataSetChanged();
                } else {
                    mYoutubeAdapter.getFilter().filter(newText.toString());
                    mYoutubeAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

    }

    //function get data from server
    private void getDataFromYoutubeApi() {
        String urlJsonObj = "https://www.googleapis.com/youtube/v3/search?pageToken=" + token_page + "&part=snippet&key=AIzaSyAOl9P42AVJLTOniUzDBw5RmC161bJ3irI";
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                urlJsonObj, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    token_page = response.getString("nextPageToken");
                    JSONArray jsonArray = response.getJSONArray("items");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = (JSONObject) jsonArray.get(i);
                        YoutubeModel ym = new YoutubeModel();
                        ym.setTitle(item.getJSONObject("snippet").getString("title"));
                        ym.setThumbnails(item.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                        list.add(ym);
                    }
                    if (count == 0) {
                        mYoutubeAdapter = new YoutubeAdapter();
                        mRecyclerView.setAdapter(mYoutubeAdapter);
                        count = 1;
                    } else {
                        mYoutubeAdapter.notifyDataSetChanged();
                        mYoutubeAdapter.setLoaded();
                    }
                    mYoutubeAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                        @Override
                        public void onLoadMore() {
                            list.add(null);
                            mYoutubeAdapter.notifyItemInserted(list.size() - 1);

                            //Load more data for reyclerview
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Remove loading item
                                    list.remove(list.size() - 1);
                                    mYoutubeAdapter.notifyItemRemoved(list.size());

                                    //Load data
                                    getDataFromYoutubeApi();

                                }
                            }, 2000);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //   mYoutubeAdapter.setLoaded();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("MainAcitivty", "Error: " + error.getMessage());

            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }


    //-----------------------------------------------------------------------------------
    //Class RecyclerView Adapter
    class YoutubeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
        private final int VIEW_TYPE_ITEM = 0;
        private final int VIEW_TYPE_LOADING = 1;

        private OnLoadMoreListener mOnLoadMoreListener;
        private List<YoutubeModel> orig;
        private boolean isLoading;
        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;

        public YoutubeAdapter() {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (mOnLoadMoreListener != null) {
                            mOnLoadMoreListener.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });
        }

        public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
            this.mOnLoadMoreListener = mOnLoadMoreListener;
        }

        @Override
        public int getItemViewType(int position) {
            return list.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_user_item, parent, false);
                return new YoutubeViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_loading_item, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof YoutubeViewHolder) {
                YoutubeModel YoutubeModel = list.get(position);
                YoutubeViewHolder youtubeViewHolder = (YoutubeViewHolder) holder;
                youtubeViewHolder.tvName.setText(YoutubeModel.getTitle());
                Picasso.with(getApplicationContext()).load(YoutubeModel.getThumbnails()).into(youtubeViewHolder.imageView);

            } else if (holder instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }


        public void setLoaded() {
            isLoading = false;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    final FilterResults oReturn = new FilterResults();
                    final List<YoutubeModel> results = new ArrayList<>();
                    if (orig == null)
                        orig = list;
                    if (charSequence != null) {
                        if (orig != null & orig.size() > 0) {
                            for (final YoutubeModel g : orig) {
                                if (g.getTitle().toLowerCase().contains(charSequence.toString()))
                                    results.add(g);
                            }
                        }
                        oReturn.values = results;
                    }
                    return oReturn;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    list = (ArrayList<YoutubeModel>) filterResults.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    static class YoutubeViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public ImageView imageView;

        public YoutubeViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tvTitle);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);

        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar1);
        }
    }
}
