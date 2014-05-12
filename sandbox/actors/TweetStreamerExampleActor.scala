import twitter4j._
import akka.actor.Actor

/**
 * A simple example of the streaming API based on specific keyword searches.
 * Simply print the text content of the tweets.
 *
 * @see TweetStreamemerExampleSpec to launch it.
 * 		command : play testOnly TweetStreamerExampleSpec
 * @pre Add the required keys in the conf. Subscribe : https://apps.twitter.com/
 */
class TweetStreamExampleActor extends Actor {

  val conf = new twitter4j.conf.ConfigurationBuilder()
    .setOAuthConsumerKey(???)
    .setOAuthConsumerSecret(???)
    .setOAuthAccessToken(???)
    .setOAuthAccessTokenSecret(???)
    .build

  def printerListener = new StatusListener() {
    /* Just dummy print of the text of the tweet + geo */
    def onStatus(s: Status) = println(s.getText + " : " + s.getGeoLocation)

    /* Empty methods required to extend StatusListener */
    def onDeletionNotice(sdn: StatusDeletionNotice) = {}
    def onTrackLimitationNotice(nlm: Int) = {}
    def onException(e: Exception) = { e.printStackTrace }
    def onScrubGeo(a: Long, b: Long) = {}
    def onStallWarning(warn: StallWarning) = {}
  }

  def receive = {
    case strs: Array[String] =>
      println("Launching the streamer...")
      val stream = new TwitterStreamFactory(conf).getInstance
      stream.addListener(printerListener)
      stream.filter(new FilterQuery().track(strs))

    case _ => sys.error("Not a valid input for the Streamer Example !")
  }
}