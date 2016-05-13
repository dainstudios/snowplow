/*
 * Copyright (c) 2014-2016 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.hadoop.scalding

// Specs2
import org.specs2.mutable.Specification

// Scalding
import com.twitter.scalding.{JsonLine => StandardJsonLine, _}

// Cascading
import cascading.tuple.Fields
import cascading.tap.SinkMode

// Commons
import org.apache.commons.codec.binary.Base64
import java.nio.charset.StandardCharsets.UTF_8

class SnowplowEventRecoveryJobSpec extends Specification {
  import Dsl._

  "A SnowplowEventRecovery job" should {

    val identity = """
      function process(event, errors) {
        return arrayToTsv(tsvToArray(event));
      }
    """

    val changeMethod = """
      function process(event, errors) {
        var fields = tsvToArray(event);
        fields[5] = 'POST';
        return arrayToTsv(fields);
      }
    """

    val discardByError = """
      function process(event, errors) {
        if (errors.length == 1 && /schema not found/.test(errors[0])) {
          return event;
        } else {
          return null;
        }
      }
    """

    val changeQuerystring = """
      function process(event, errors) {
        var fields = tsvToArray(event);
        var querystringDict = parseQuerystring(fields[11]);
        querystringDict['tv'] = 'js-2.7.0';
        fields[11] = buildQuerystring(querystringDict);
        return arrayToTsv(fields);
      }
    """

    JobTest("com.snowplowanalytics.hadoop.scalding.SnowplowEventRecoveryJob")
      .arg("input", "inputFile")
      .arg("output", "outputFile")
      .arg("script", new String(Base64.encodeBase64(identity.getBytes()), UTF_8))
      .source(MultipleTextLineFiles("inputFile"), List((0, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"Field [e]: [psp] is not a recognised event code"},{"level":"error","message":"Unrecognized event [null]"}]}""")))
      .sink[String](Tsv("outputFile")) { buf =>
        "extract the line" in {
          buf.size must_== 1
          buf.head must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
        }
      }
      .run
      .finish

    JobTest("com.snowplowanalytics.hadoop.scalding.SnowplowEventRecoveryJob")
      .arg("input", "inputFile")
      .arg("output", "outputFile")
      .arg("script", new String(Base64.encodeBase64(changeMethod.getBytes()), UTF_8))
      .source(MultipleTextLineFiles("inputFile"), List((0, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"Field [e]: [psp] is not a recognised event code"},{"level":"error","message":"Unrecognized event [null]"}]}""")))
      .sink[String](Tsv("outputFile")) { buf =>
        "mutate the raw event" in {
          buf.size must_== 1
          buf.head must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tPOST\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
        }
      }
      .run
      .finish

    JobTest("com.snowplowanalytics.hadoop.scalding.SnowplowEventRecoveryJob")
      .arg("input", "inputFile")
      .arg("output", "outputFile")
      .arg("script", new String(Base64.encodeBase64(changeQuerystring.getBytes()), UTF_8))
      .source(MultipleTextLineFiles("inputFile"), List((0, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"Field [e]: [psp] is not a recognised event code"},{"level":"error","message":"Unrecognized event [null]"}]}""")))
      .sink[String](Tsv("outputFile")) { buf =>
        "mutate the querystring" in {
          buf.size must_== 1
          buf.head must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=js-2.7.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
        }
      }
      .run
      .finish

    JobTest("com.snowplowanalytics.hadoop.scalding.SnowplowEventRecoveryJob")
      .arg("input", "inputFile")
      .arg("output", "outputFile")
      .arg("script", new String(Base64.encodeBase64(discardByError.getBytes()), UTF_8))
      .source(MultipleTextLineFiles("inputFile"), List(
        (0, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"Field [e]: [psp] is not a recognised event code"},{"level":"error","message":"Unrecognized event [null]"}]}"""),
        (1, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"schema not found"}]}"""),
        (2, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"schema not found"},{"level":"error","message":"Unrecognized event [null]"}]}""")
        ))
      .sink[String](Tsv("outputFile")) { buf =>
        "filter based on error messages" in {
          buf.size must_== 1
          buf.head must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
        }
      }
      .run
      .finish

    JobTest("com.snowplowanalytics.hadoop.scalding.SnowplowEventRecoveryJob")
      .arg("input", "inputFile")
      .arg("output", "outputFile")
      .arg("script", new String(Base64.encodeBase64(changeMethod.getBytes()), UTF_8))
      .source(MultipleTextLineFiles("inputFile"), List(
        (0, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":[{"level":"error","message":"Field [e]: [psp] is not a recognised event code"}]}"""),
        (1, """{"line":"2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tGET\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001","errors":["Field [e]: [psp] is not a recognised event code"]}""")
        ))
      .sink[String](Tsv("outputFile")) { buf =>
        "accept both old-style and new-style bad row formats" in {
          buf.size must_== 2
          buf(0) must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tPOST\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
          buf(1) must be_==("2014-04-28\t18:13:40\tJFK1\t831\t76.9.199.178\tPOST\td3v6ndkyapxc2w.cloudfront.net\t/i\t200\thttp://snowplowanalytics.com/analytics/customer-analytics/cohort-analysis.html\tMozilla/5.0%2520(Macintosh;%2520Intel%2520Mac%2520OS%2520X%252010_9_2)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/34.0.1847.131%2520Safari/537.36\te=pp&page=Cohort%2520Analysis&pp_mix=0&pp_max=0&pp_miy=1999&pp_may=1999&cx=eyJwYWdlIjp7InVybCI6ImFuYWx5dGljcyJ9fQ&dtm=1398708819267&tid=319357&vp=1436x783&ds=1436x6689&vid=4&duid=d91cd3ae94725999&p=web&tv=2.0.0&fp=985410387&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%252FNew_York&tna=cloudfront&evn=com.snowplowanalytics&refr=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fattribution.html&f_pdf=1&f_qt=1&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1920x1080&cd=24&cookie=1&url=http%253A%252F%252Fsnowplowanalytics.com%252Fanalytics%252Fcustomer-analytics%252Fcohort-analysis.html\t-\tHit\t1x0ytHOrKiCpOY9JW7UmBUm7_P1LNVgzZexm42vCShxJcUlfp8EMOw==\td3v6ndkyapxc2w.cloudfront.net\thttp\t1014\t0.001")
        }
      }
      .run
      .finish
  }
}
