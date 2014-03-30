package models

abstract class TweetListener {
  def act(tweet: Tweet)
}