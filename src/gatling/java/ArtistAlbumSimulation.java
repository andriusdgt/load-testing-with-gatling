import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ArtistAlbumSimulation extends Simulation {
    FeederBuilder.Batchable<String> feeder = csv("artists.csv").random();
    ScenarioBuilder artistsScenario = scenario("Query artists and albums scenario")
            .feed(feeder)
            .exec(http("Get artists")
                    .get("/artist/name/${artistName}")
                    .header("Accept", "application/json")
                    .check(status().is(200))
                    .check(jsonPath("$[0].amgId").saveAs("artistId"))
            )
            .exec(http("Get top albums")
                    .get("/artist/${artistId}/album/top")
                    .header("Accept", "application/json")
                    .check(status().is(200))
            );

    HttpProtocolBuilder httpProtocol = http.baseUrl("http://localhost:8080");

    {
        setUp(artistsScenario.injectOpen(stressPeakUsers(10000).during(40)))
                .protocols(httpProtocol)
                .assertions(
                        global().successfulRequests().percent().shouldBe(100.0),
                        global().responseTime().max().lt(1000),
                        global().responseTime().percentile4().lt(100),
                        global().responseTime().percentile2().lt(50)
                );
    }
}
