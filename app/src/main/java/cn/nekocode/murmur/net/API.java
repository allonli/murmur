package cn.nekocode.murmur.net;

import android.text.Html;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.StringRequest;
import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVCloud;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVOnlineConfigureListener;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.nekocode.murmur.MyApplication;
import cn.nekocode.murmur.beans.MurmurBean;
import cn.nekocode.murmur.beans.SongBean;
import cn.nekocode.murmur.utils.MyCallback.Callback1;

/**
 * Created by nekocode on 2015/4/22 0022.
 */
public class API {
    private static final String API_GET_SONG = "http://music.douban.com/j/songlist/get_song_url?sid=%s&&ssid=%s";

    public static void getMurmurs(final Callback1<Map<String, MurmurBean>> callback) {
        AVQuery query = AVQuery.getQuery("Murmurs");
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                HashMap<String, MurmurBean> mapToReturn = new HashMap<String, MurmurBean>();
                for (AVObject avObject : list) {
                    MurmurBean murmurBean = new MurmurBean();
                    murmurBean.setId(avObject.getObjectId());
                    murmurBean.setName(avObject.getString("name"));
                    murmurBean.setUrl(avObject.getAVFile("file").getUrl());

                    mapToReturn.put(murmurBean.getName(), murmurBean);
                }

                callback.run(mapToReturn);
            }
        });
    }

    public static void getPlayMurmurs(final Callback1<String[]> callback) {
        AVQuery query = AVQuery.getQuery("PlayMurmurs");
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                String rlt[] = new String[list.size()];

                int i=0;
                for (AVObject avObject : list) {
                    rlt[i] = avObject.getString("name");
                    i++;
                }

                callback.run(rlt);
            }
        });
    }

    public static void getSongs(final Callback1<List<SongBean>> callback) {
        AVQuery query = AVQuery.getQuery("Songs").orderByAscending("order");
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                List<SongBean> listToReturn = new ArrayList<>();
                for (AVObject avObject : list) {
                    SongBean songBean = new SongBean();
                    songBean.setId(avObject.getObjectId());
                    songBean.setName(avObject.getString("name"));
                    songBean.setPerformer(avObject.getString("performer"));
                    songBean.setUrl(avObject.getString("url"));
                    songBean.setCoverUrl(avObject.getString("cover"));

                    listToReturn.add(songBean);
                }

                callback.run(listToReturn);
            }
        });
    }

    public static void loadSetting(final Callback1<org.json.JSONObject> callback) {
        AVAnalytics.setOnlineConfigureListener(new AVOnlineConfigureListener() {
            @Override
            public void onDataReceived(org.json.JSONObject jsonObject) {
                callback.run(jsonObject);
            }
        });
        AVAnalytics.updateOnlineConfig(MyApplication.getContext());
    }

    public static void spiderSongs(RequestQueue queue, String doubanUrl, final Callback1<List<SongBean>> callback) {
        DoubanRequest stringRequest = new DoubanRequest(doubanUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                List<SongBean> list = new ArrayList<>();

                Pattern p = Pattern.compile("class=\"song-item\"(.([\\s\\S]*?))class=\"slic song-share\">");
                Pattern p2 = Pattern.compile("data-title=\"(.*?)\"");
                Pattern p3 = Pattern.compile("data-performer=\"(.*?)\"");
                Pattern p4 = Pattern.compile("data-songid=\"(.*?)\"");
                Pattern p5 = Pattern.compile("data-ssid=\"(.*?)\"");
                Pattern p6 = Pattern.compile("data-cover=\"(.*?)\"");
                Matcher m = p.matcher(response);

                while (m.find()) {
                    String section = m.group();

                    SongBean songBean = new SongBean();


                    Matcher m2 = p2.matcher(section);
                    if(m2.find()) {
                        songBean.setName(Html.fromHtml(m2.group(1)).toString());
                    }

                    m2 = p3.matcher(section);
                    if(m2.find()) {
                        songBean.setPerformer(Html.fromHtml(m2.group(1)).toString());
                    }

                    m2 = p4.matcher(section);
                    if(m2.find()) {
                        songBean.setSid(m2.group(1));
                    }

                    m2 = p5.matcher(section);
                    if(m2.find()) {
                        songBean.setSsid(m2.group(1));
                    }

                    m2 = p6.matcher(section);
                    if(m2.find()) {
                        songBean.setCoverUrl(m2.group(1));
                    }


                    list.add(songBean);
                }

                callback.run(list);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(stringRequest);
    }

    public static void getSongUrl(RequestQueue queue, final SongBean songBean, final Callback1<SongBean> callback) {
        String api = String.format(API_GET_SONG, songBean.getSid(), songBean.getSsid());

        DoubanRequest stringRequest = new DoubanRequest(api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = JSON.parseObject(response);

                songBean.setUrl(jsonObject.getString("r"));
                callback.run(songBean);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(stringRequest);
    }
}
