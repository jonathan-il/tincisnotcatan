package edu.brown.cs.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;

public class UserGroup implements Timestamped, Group {


  private Collection<RequestProcessor> reqs;

  private Set<User>                    users;
  private API                          api;
  private long                         initTime;

  private String                       identifier;

  private final int                    desiredSize;


  private UserGroup() {
    assert false : "should never call this constructor!";
    this.desiredSize = 1;
  }


  private UserGroup(UserGroupBuilder b) {
    initTime = System.currentTimeMillis();
    // use the fields of the builder to setup
    this.reqs = b.reqs;
    this.desiredSize = b.desiredSize;
    this.identifier = b.identifier;
    // default inits
    this.users = new HashSet<>();

    if (b.apiClass != null) {
      try {
        this.api = b.apiClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        System.out.println(
            "Error instantiating API class of type:" + b.apiClass.getName());
        e.printStackTrace();
      }
    }
  }


  public String identifier() {
    return identifier;
  }


  public int maxSize() {
    return this.desiredSize;
  }


  public int currentSize() {
    return users.size();
  }


  public boolean isFull() {
    return this.desiredSize == users.size();
  }


  public boolean add(User u) {
    if (users.size() == desiredSize) {
      return false; // we're full, don't give me any more users.
    }

    u.setUserID(api.addPlayer(u.getFieldsAsJson()));

    users.add(u);
    for (User other : users) {
      JsonObject gs = api.getGameState(other.userID());
      gs.addProperty("requestType", "getGameState");
      other.message(gs);
    }
    return true;
    // regardless of whether or not u was present in the set already,
    // should return true to indicate that u has "found a home"
  }


  public boolean remove(User u) {
    return users.remove(u); // could it be this simple? we can add more logic
                            // for timeouts etc later.
  }


  public boolean handleMessage(User u, JsonObject j) {
    if (!users.contains(u)) {
      System.out.println("Error : user not contained");
      return false;
    }
    for (RequestProcessor req : reqs) {
      if (req.match(j)) {
        return req.run(u, users, j, api);
      }
    }
    return false;
  }


  public static class UserGroupBuilder {

    private Collection<RequestProcessor> reqs;
    private int                          desiredSize = 1;
    private final Class<? extends API>   apiClass;
    private String                       identifier  = null;


    public UserGroupBuilder(Class<? extends API> apiClass) {
      // set any required variables
      this.apiClass = apiClass;

      // default to echo processor for now
      Collection<RequestProcessor> r = new ArrayList<>();
      r.add(new EchoProcessor()); // for testing;
      this.reqs = r;
    }


    public UserGroupBuilder withRequestProcessors(
        Collection<RequestProcessor> reqs) {
      this.reqs = reqs;
      return this;
    }


    public UserGroupBuilder withSize(int numUsers) {
      this.desiredSize = numUsers;
      return this;
    }


    public UserGroupBuilder withUniqueIdentifier(String id) {
      this.identifier = id;
      return this;
    }


    public UserGroup build() {
      return new UserGroup(this);
    }
  }


  @Override
  public long initTime() {
    return initTime;
  }


  @Override
  public void stampNow() {
    this.initTime = System.currentTimeMillis();

  }


  @Override
  public boolean isEmpty() {
    return users.isEmpty();
  }
}