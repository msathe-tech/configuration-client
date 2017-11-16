package hello;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@SpringBootApplication
public class ConfigClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigClientApplication.class, args);
    }
}

@RefreshScope
@RestController
class MessageRestController {
    private static Logger log = LoggerFactory.getLogger(MessageRestController.class);
    
    @Autowired
    private AlbumRepository ar;

	private ArrayList<Album> albums;
	private JedisPool pool;

    @Value("${message:Hello default}")
    private String message;
    
    @Value("${store:RDBMS}")
    private String store;
    
  
    @RequestMapping("/message")
    String getMessage() {
        return this.message;
    }
    
    @ResponseStatus(code=HttpStatus.CREATED)
    @PostMapping(value="/add-albums")
    public String addAlbums(@RequestBody ArrayList<Album> list) {
      log.info("addAlbums....." + store);
      
      if(store.equals("CACHE")) {
    	  return cacheAlbums(list);
      } else if (store.equals("RDBMS")) {
    	  return persistAlbums(list);
      }
      return "Store type not found";
    }
    
    @GetMapping(value="/getAlbum")
    public String getAlbum(@RequestParam(value="title") String title) {
    	Album album = ar.findOne(title);
    	if(album == null) {
    		String error = "Album not found for title: " + title;
			log.error(error);
			return error;
    	} 
    	else {
    		log.info("Album found");
    		log.info(album.toString());
    		return album.toString();
    	}
    }
    
    @GetMapping(value="/countAlbums")
    public String countAlbum() {
    	
    	if(store.equals("RDBMS")) {
    		List<Album> albums = (List<Album>) ar.findAll();
        	
        	if(albums == null) {
        		String error = "There are no albums in DB";
    			log.error(error);
    			return error;
        	} 
        	else {
        		log.info("Albums found: " + albums.size());
        		String titles = "Total albums in DB: " + albums.size();
        		albums.forEach(album -> log.info("Title (DB): " + album.getTitle()));
        		return titles;
        	}
    	} else if(store.equals("CACHE")) {
    		try {
        	    String vcap_services = System.getenv("VCAP_SERVICES");
        	    log.info("VCAP_SERVICES - \n" + vcap_services);
        	    if (vcap_services != null && vcap_services.length() > 0) {
        	        // parsing rediscloud credentials
        	    	JSONObject rootJsonObject = new JSONObject(vcap_services);
        	    	
        	    	log.info("rootJsonObject json: " + rootJsonObject.toString());
        	    	JSONArray rediscloudNode = rootJsonObject.getJSONArray("rediscloud");
        	    	
        	        log.info("rediscloud Json node: " + (rediscloudNode == null));
        	        JSONObject credentials = getJsonObjectFromArray(rediscloudNode, "credentials");
        	        
        	        log.info("credentials Json node: \n" + credentials.toString());	
        	        pool = new JedisPool(new JedisPoolConfig(),
        	                credentials.getString("hostname"),
        	                Integer.parseInt(credentials.getString("port")),
        	                Protocol.DEFAULT_TIMEOUT,
        	                credentials.getString("password")); 
        	        if (pool == null) {
        	        	log.error("jedis pool not created....");
        	        }
        	    }
        	} catch (Exception ex) {
        	    // vcap_services could not be parsed.
        		log.error("vcap_services could not be parsed." + ex.getMessage());
        		ex.printStackTrace();
        	}
        	
        	Jedis jedis = pool.getResource();
        	Set<String> keys = jedis.keys("*");
        	if(keys != null) {
        		keys.forEach(title -> log.info("Title (cache): " + title));
            	String titles = "Total albums in cache: " + keys.size();
            	return titles;
        	} else {
        		log.error("No title found in cache");
        		return "No title found in cache";
        	}
        	
       }
		return "No clue what happened, can't find the store";
    	
    }
    
    @GetMapping("/kill-instance")
	public void die()
	{
		log.error("This world does not compute!  I'm ending it all now!!");
		System.exit(-1);
	}
    
   /* @PostMapping("/cacheAlbums") 
    public String cacheAlbums(@RequestBody ArrayList<Album> list) {
    	log.info("/cacheAlbums.....");
    	try {
    	    String vcap_services = System.getenv("VCAP_SERVICES");
    	    log.info("VCAP_SERVICES - \n" + vcap_services);
    	    if (vcap_services != null && vcap_services.length() > 0) {
    	        // parsing rediscloud credentials
    	    	JSONObject rootJsonObject = new JSONObject(vcap_services);
    	    	
    	    	log.info("rootJsonObject json: " + rootJsonObject.toString());
    	    	JSONArray rediscloudNode = rootJsonObject.getJSONArray("rediscloud");
    	    	
    	        log.info("rediscloud Json node: " + (rediscloudNode == null));
    	        JSONObject credentials = getJsonObjectFromArray(rediscloudNode, "credentials");
    	        
    	        log.info("credentials Json node: \n" + credentials.toString());	
    	        pool = new JedisPool(new JedisPoolConfig(),
    	                credentials.getString("hostname"),
    	                Integer.parseInt(credentials.getString("port")),
    	                Protocol.DEFAULT_TIMEOUT,
    	                credentials.getString("password")); 
    	        if (pool == null) {
    	        	log.error("jedis pool not created....");
    	        }
    	    }
    	} catch (Exception ex) {
    	    // vcap_services could not be parsed.
    		log.error("vcap_services could not be parsed." + ex.getMessage());
    		ex.printStackTrace();
    	}
    	
    	Jedis jedis = pool.getResource();
    	
    	  this.albums = list;
    	  if(this.albums != null && this.albums.size() > 0) {
    		  Album newAlbum = null;
    		  Album album = null; 
    		  String savedAlbum = null;
    		  for(int i = 0; i < albums.size(); i++) {
    			  album = (Album) albums.get(i);
    			  log.info("Got album - " + album.getTitle());
    			  newAlbum = new Album(album.getTitle(), album.getArtist(), album.getReleaseYear(), album.getGenre());
    			  log.info("Create new album instance - " + newAlbum.getTitle());
    			  log.info("Trying to cache new album");
    			  try {
    				savedAlbum = jedis.set(newAlbum.getTitle(), newAlbum.toString());
    			  } catch (Exception e) {
    				  log.error(e.getMessage());
    			  }
    			  if (savedAlbum == null) {
    				  log.error("Failed to cache album: " + newAlbum.getTitle());
    			  } else {
    				  log.info("Album saved to cache: " + newAlbum.getTitle());
    			  }
    		  }
    		  return "Albums added: " + albums.size();
    	  }
    	  else {
    		  log.error("No album found in request");
    		  return "No album found in request";
    	  }
    }*/
    
    private String persistAlbums(ArrayList<Album> list) {
    	this.albums = list;
    	  if(this.albums != null && this.albums.size() > 0) {
    		  Album newAlbum = null;
    		  Album album = null; 
    		  Album savedAlbum = null;
    		  for(int i = 0; i < albums.size(); i++) {
    			  album = (Album) albums.get(i);
    			  log.info("Got album - " + album.getTitle());
    			  newAlbum = new Album(album.getTitle(), album.getArtist(), album.getReleaseYear(), album.getGenre());
    			  log.info("Create new album instance - " + newAlbum.getTitle());
    			  log.info("Trying to save new album");
    			  try {
    				  savedAlbum = (Album) ar.save(newAlbum);
    			  } catch (Exception e) {
    				  log.error(e.getMessage());
    			  }
    			  log.info("Check if album is saved to repo...");
    			  if (savedAlbum == null) {
    				  log.error("Failed to save album for title: " + newAlbum.getTitle());
    				  return "Failed to save album for title: " + newAlbum.getTitle();
    			  } else {
    				  log.info("Album created for title: " + savedAlbum.getTitle());
    			  }
    		  }
    		  return "Albums added: " + albums.size();
    	  }
    	  else {
    		  log.error("No album found in request");
    		  return "No album found in request";
    	  }
    }
    
    private String cacheAlbums(ArrayList<Album> list) {
    	log.info("cacheAlbums.....");
    	try {
    	    String vcap_services = System.getenv("VCAP_SERVICES");
    	    log.info("VCAP_SERVICES - \n" + vcap_services);
    	    if (vcap_services != null && vcap_services.length() > 0) {
    	        // parsing rediscloud credentials
    	    	JSONObject rootJsonObject = new JSONObject(vcap_services);
    	    	
    	    	log.info("rootJsonObject json: " + rootJsonObject.toString());
    	    	JSONArray rediscloudNode = rootJsonObject.getJSONArray("rediscloud");
    	    	
    	        log.info("rediscloud Json node: " + (rediscloudNode == null));
    	        JSONObject credentials = getJsonObjectFromArray(rediscloudNode, "credentials");
    	        
    	        log.info("credentials Json node: \n" + credentials.toString());	
    	        pool = new JedisPool(new JedisPoolConfig(),
    	                credentials.getString("hostname"),
    	                Integer.parseInt(credentials.getString("port")),
    	                Protocol.DEFAULT_TIMEOUT,
    	                credentials.getString("password")); 
    	        if (pool == null) {
    	        	log.error("jedis pool not created....");
    	        }
    	    }
    	} catch (Exception ex) {
    	    // vcap_services could not be parsed.
    		log.error("vcap_services could not be parsed." + ex.getMessage());
    		ex.printStackTrace();
    	}
    	
    	Jedis jedis = pool.getResource();
    	
    	  this.albums = list;
    	  if(this.albums != null && this.albums.size() > 0) {
    		  Album newAlbum = null;
    		  Album album = null; 
    		  String savedAlbum = null;
    		  for(int i = 0; i < albums.size(); i++) {
    			  album = (Album) albums.get(i);
    			  log.info("Got album - " + album.getTitle());
    			  newAlbum = new Album(album.getTitle(), album.getArtist(), album.getReleaseYear(), album.getGenre());
    			  log.info("Create new album instance - " + newAlbum.getTitle());
    			  log.info("Trying to cache new album");
    			  try {
    				savedAlbum = jedis.set(newAlbum.getTitle(), newAlbum.toString());
    			  } catch (Exception e) {
    				  log.error(e.getMessage());
    			  }
    			  if (savedAlbum == null) {
    				  log.error("Failed to cache album: " + newAlbum.getTitle());
    			  } else {
    				  log.info("Album saved to cache: " + newAlbum.getTitle());
    			  }
    		  }
    		  return "Albums cached: " + albums.size();
    	  }
    	  else {
    		  log.error("No album found in request");
    		  return "No album found in request";
    	  }
    }
    
    private JSONObject getJsonObjectFromArray(JSONArray jsonArray, String key) throws JSONException {
        for(int index = 0; index < jsonArray.length(); index++) {
            JSONObject jsonObject = jsonArray.getJSONObject(index);
            log.info(index + ": " + jsonObject.toString());
            if(jsonObject.getJSONObject(key) != null) {
            	log.info("getting tired now, please work");
            	log.info("jsonObject.getJSONObject(" + key + ") " + jsonObject.getJSONObject(key).toString()); 
                return jsonObject.getJSONObject(key); //this is the index of the JSONObject you want
            } 
        }
        return null; //it wasn't found at all
    }
}


@Entity
class Album {

	@Id
	private String title;
	
	private String artist;
	private int releaseYear;
	private String genre;
	
	public Album() {
		
	}
	
	public Album(String title) {
		this.title = title;
	}
	
	public Album(String title, String artist, int releaseYear, String genre) {
		this.title = title;
		this.artist = artist;
		this.releaseYear = releaseYear;
		this.genre = genre;
	}
	
	public String toString() {
		return "Album: \n" + title + "\n" + artist + "\n" + releaseYear + "\n" + genre;
	}
	
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getReleaseYear() {
		return releaseYear;
	}
	public void setReleaseYear(int releaseYear) {
		this.releaseYear = releaseYear;
	}
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	
}
