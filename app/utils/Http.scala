package utils

import org.apache.http._
import org.apache.http.entity.StringEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.BufferedOutputStream
import java.io.InputStream
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer

object Http {

  /** Ask for a Get request ot the URL specified. The request might be signed by the specified consumer. */
  def askForGet(request: String, consumer: Option[CommonsHttpOAuthConsumer] = None) = {
    val httpRequest = new HttpGet(request)
    httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
    val client = new DefaultHttpClient()
    if (consumer.isDefined) consumer.get.sign(httpRequest) /* If required, sign the query with the proper client's OAuth */
    val response = client.execute(httpRequest) /* Send the request and get the response */
    response.getEntity().getContent()
  }

  /** Read all the data present in the InputStream in and return it as a String */
  def readAll(in: InputStream): String = {
    val inr = new BufferedReader(new InputStreamReader(in))
    val bf = new StringBuilder
    var rd = inr.readLine
    while (rd != null) { bf.append(rd); rd = inr.readLine }
    bf.toString
  }
}

