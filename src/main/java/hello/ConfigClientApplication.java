package hello;

import java.util.ArrayList;
import java.util.List;
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
    
  
    @RequestMapping("/message")
    String getMessage() {
        return this.message;
    }
    
    @ResponseStatus(code=HttpStatus.CREATED)
    @PostMapping(value="/add-albums")
    public String addAlbums(@RequestBody ArrayList<Album> list) {
      log.info("addAlbums.....");
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
    	List<Album> albums = (List<Album>) ar.findAll();
    	
    	if(albums == null) {
    		String error = "There are no albums";
			log.error(error);
			return error;
    	} 
    	else {
    		log.info("Albums found: " + albums.size());
    		String titles = "Total albums: " + albums.size();
    		albums.forEach(album -> log.info("Title: " + album.getTitle()));
    		return titles;
    	}
    }
    
    @GetMapping("/kill-instance")
	public void die()
	{
		log.error("This world does not compute!  I'm ending it all now!!");
		System.exit(-1);
	}
    
    @PostMapping("/cacheAlbums") 
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

