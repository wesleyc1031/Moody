package com.example.moodymusic;

import java.util.*;
import java.lang.*;
import com.example.moodymusic.LoginActivity;
import com.example.moodymusic.MainActivity;
import android.support.v4.app.*;
import kaaes.spotify.webapi.android.annotations.DELETEWITHBODY;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Albums;
import kaaes.spotify.webapi.android.models.AlbumsPager;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.ArtistsCursorPager;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.CategoriesPager;
import kaaes.spotify.webapi.android.models.Category;
import kaaes.spotify.webapi.android.models.FeaturedPlaylists;
import kaaes.spotify.webapi.android.models.NewReleases;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistFollowPrivacy;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.Recommendations;
import kaaes.spotify.webapi.android.models.Result;
import kaaes.spotify.webapi.android.models.SavedAlbum;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.SeedsGenres;
import kaaes.spotify.webapi.android.models.SnapshotId;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.TracksPager;
import kaaes.spotify.webapi.android.models.TracksToRemove;
import kaaes.spotify.webapi.android.models.TracksToRemoveWithPosition;
import kaaes.spotify.webapi.android.models.UserPrivate;
import kaaes.spotify.webapi.android.models.UserPublic;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import kaaes.spotify.webapi.android.models.UserPublic;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.os.Bundle;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import kaaes.spotify.webapi.android.models.UserPublic;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SpotifyService2 {

    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "8d90322721dc44d0a6d23ee72c458d91";
    private static final String REDIRECT_URI = "com.example.moodymusic://callback";
    private SpotifyApi api = new SpotifyApi();
    private SpotifyService spotify;
    private AuthenticationRequest request;
    private ArrayList<String> playlistNameList = new ArrayList<>();
    private ArrayList<String> playlistIDList = new ArrayList<>();
    private ArrayList<String> playlistCreator = new ArrayList<>();
    private ArrayList<String> topArtistIDs = new ArrayList<>();
    private ArrayList<String> tracksRecommended = new ArrayList<>();
    private String userID;
    private int playlistChosenPosition;
    private Playlist playlistObj;
    private Pager<Recommendations> recommendationsObj;
    private String token;

    protected void onCreate(Bundle savedInstanceState) {

        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN,
                        REDIRECT_URI);
        builder.setScopes(new String[]{"user-top-read", "playlist-modify-private", "playlist-modify-public", "user-modify-playback-state"});
        builder.setShowDialog(true);
        request = builder.build();
    }

    private void startSpotifyService() {
        api.setAccessToken(token);
        spotify = api.getService();
    }

    public void createNewPlaylist(final String userIdValue){
        final Map<String, Object> options = new HashMap<>();
        userID = userIdValue;
        options.put("name", "Moody - Happy");
        options.put("public", true);
        options.put("collaborative", false);
        options.put("description", "Auto-Generated Playlist");
        spotify.createPlaylist(userIdValue, options, new Callback<Playlist>(){
            @Override
            public void success(final Playlist playlist, Response response) {
                playlistObj = playlist;
                playlistNameList.add(playlistObj.name);
                playlistIDList.add(playlistObj.id);
                playlistCreator.add(playlistObj.owner.display_name);
                Map<String, Object> playlistTracks = new HashMap<>();
                spotify.getPlaylist(userIdValue, playlistIDList.get(playlistChosenPosition), playlistTracks, new Callback<Playlist>() {
                    @Override
                    public void success(final Playlist playlist, Response response) {
                    }

                    @Override
                    public void failure(RetrofitError error) {
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });
    }
//
    public void getTopArtists(final String userIdValue){
        userID = userIdValue;
        final Map<String, Object>  options = new HashMap<>();
        String time_range = "short_term";
        options.put(SpotifyService.LIMIT, 4);
        options.put(SpotifyService.TIME_RANGE, time_range);
        spotify.getTopArtists(options, new Callback<Pager<Artist>>(){
            @Override
            public void success(Pager<Artist> artistPager, Response response) {
                if(artistPager.total > 0){
                    for(int i = 0; i < artistPager.items.size(); i++){
                        topArtistIDs.add(artistPager.items.get(i).id);
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });
    }

    public void getRecommendatinos(final String userIdValue){
        final Map<String, Object> options = new HashMap<>();
        userID = userIdValue;
        String genre = "happy";
        options.put(SpotifyService.LIMIT, 100);
        options.put(SpotifyService.FIELDS, genre);
        options.put(SpotifyService.FIELDS, topArtistIDs);
        spotify.getRecommendations(userIdValue, options, new Callback <Pager<Recommendations>>(){
            @Override
            public void success(final Pager<Recommendations> recommendations, Response response) {
                recommendationsObj = recommendations;
                tracksRecommended.add(recommendationsObj.id);
            }
            @Override
            public void failure(RetrofitError error) {
            }
        });
    }

    public void addTracksToPlaylist(String userIdValue, Playlist playlist, Pager<PlaylistTrack> playlistTrackPager){
        for (int i = 0; i < playlistTrackPager.items.size(); i++) {
            tracksRecommended.add(playlistTrackPager.items.get(i).track.uri.toString());
        }
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("uris", (tracksRecommended));
        spotify.addTracksToPlaylist(userIdValue, playlist.id, query, body,
                new SpotifyCallback<Pager<PlaylistTrack>>() {
                    @Override
                    public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                    }

                    @Override
                    public void failure(SpotifyError spotifyError) {
                    }
                });
    }}
