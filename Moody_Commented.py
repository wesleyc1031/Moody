import spotipy; import spotipy.util as util; import spotipy.oauth2

username='alostvagabond'
playlist_name='Moody - Happy'

scope = 'user-top-read playlist-modify-private playlist-modify-public user-modify-playback-state'
token = util.prompt_for_user_token(username,scope,client_id='8d90322721dc44d0a6d23ee72c458d91',client_secret='3523fb0d93c44e5ab51bd4c609ef057c',redirect_uri='http://localhost/')
sp = spotipy.Spotify(auth=token)

range = ['short_term'] #ranges = ['short_term', 'medium_term', 'long_term']; We use short because it's most recent
genre = ['happy'] #"genres":[ "acoustic", "afrobeat", "alt-rock", "alternative", "ambient", "anime", "black-metal", "bluegrass", "blues", "bossanova", "brazil", "breakbeat", "british", "cantopop", "chicago-house", "children", "chill", "classical", "club", "comedy", "country", "dance", "dancehall", "death-metal", "deep-house", "detroit-techno", "disco", "disney", "drum-and-bass", "dub", "dubstep", "edm", "electro", "electronic", "emo", "folk", "forro", "french", "funk", "garage", "german", "gospel", "goth", "grindcore", "groove", "grunge", "guitar", "happy", "hard-rock", "hardcore", "hardstyle", "heavy-metal", "hip-hop", "holidays", "honky-tonk", "house", "idm", "indian", "indie", "indie-pop", "industrial", "iranian", "j-dance", "j-idol", "j-pop", "j-rock", "jazz", "k-pop", "kids", "latin", "latino", "malay", "mandopop", "metal", "metal-misc", "metalcore", "minimal-techno", "movies", "mpb", "new-age", "new-release", "opera", "pagode", "party", "philippines-opm", "piano", "pop", "pop-film", "post-dubstep", "power-pop", "progressive-house", "psych-rock", "punk", "punk-rock", "r-n-b", "rainy-day", "reggae", "reggaeton", "road-trip", "rock", "rock-n-roll", "rockabilly", "romance", "sad", "salsa", "samba", "sertanejo", "show-tunes", "singer-songwriter", "ska", "sleep", "songwriter", "soul", "soundtracks", "spanish", "study", "summer", "swedish", "synth-pop", "tango", "techno", "trance", "trip-hop", "turkish", "work-out", "world-music" ]

def get_top_artists(): #Get top artists
    results = sp.current_user_top_artists(time_range=range, limit=4) #Get the user's top 4 artists they've been listening to recently
    for item in (results['items']):
        top_artists.append(item['id']) #For each of the artists, we take their artist ID and add that to an array

def get_recommendations_from_artists(): #Use the top artists to generate tracks
    results = sp.recommendations(seed_artists = top_artists, seed_genres = genre, limit=50) #Only 5 seeds may be used at any time, e.g. 4 artists and 1 genre; limit can be 1 to 100, however a playlist can be at max 100 tracks
    for track in results['tracks']:
        track_ids.append(track['id'])#This pulls the tracks's id's and puts them in an array

def get_top_tracks(): #Get top tracks
    results = sp.current_user_top_tracks(time_range=range, limit=4)
    for item in (results['items']):
        top_tracks.append(item['id'])

def get_recommendations_from_tracks(): #Use the top tracks to generate tracks
    results = sp.recommendations(seed_tracks = top_tracks, seed_genres = genre, limit=50)
    for track in results['tracks']:
        track_ids.append(track['id'])

def create_playlist(): #Create a playlist
    playlists = sp.user_playlist_create(username, playlist_name, public=True) #Creates a playlist with the credentials above, can be set to private if the user requests it
    playlist_id.append(playlists['id']) #Gets the id of the playlist
    playlist_uri.append(playlists['uri']) #Gets the uri of the playlist

def add_to_playlist(): #Add the tracks to the playlist
    sp.user_playlist_add_tracks(username, playlist_id, track_ids)

def playback(): #Start playback of the playlist
    sp.start_playback(context_uri=playlist_uri)

top_artists = []; top_tracks = []; track_ids = []; playlist_id = []; playlist_uri = []

get_top_artists()
get_top_tracks()
get_recommendations_from_artists()
get_recommendations_from_tracks()
create_playlist()
playlist_id = ''.join(playlist_id) #converts the array to a string
playlist_uri = ''.join(playlist_uri)
add_to_playlist()
#playback()