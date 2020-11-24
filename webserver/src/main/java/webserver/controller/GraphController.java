package webserver.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.spark.graphx.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import webserver.graph.PlayerHeroGraph;
import webserver.graph.PlayerPlayerGraph;
import webserver.util.ResultMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GraphController {

    @Autowired
    private PlayerHeroGraph playerHeroGraph;

    @Autowired
    private PlayerPlayerGraph playerPlayerGraph;

    @GetMapping("/get_graph")
    public ResultMsg getGraph(@RequestParam String graph,
                              @RequestParam(required = false) String players,
                              @RequestParam(required = false) String heroes,
                              @RequestParam(required = false) Integer lowerWins,
                              @RequestParam(required = false) Integer lowerMatches,
                              @RequestParam(required = false) Boolean stronglyConnected) {
        Map<String, Object> conditions = new HashMap<>();
        if (players != null) conditions.put("players", players);
        if (heroes != null) conditions.put("heroes", heroes);
        if (lowerWins != null) conditions.put("lowerWins", lowerWins);

        if (graph.equals("player_hero")) {
            if (lowerMatches == null)
                conditions.put("lowerMatches", PlayerHeroGraph.lowestMatches);
            else
                conditions.put("lowerMatches",
                        lowerMatches > PlayerHeroGraph.lowestMatches ? lowerMatches : PlayerHeroGraph.lowestMatches);

            JSONObject graphinfo = playerHeroGraph.graph2JSON(playerHeroGraph.getGraph(conditions));
            return new ResultMsg(ResultMsg.SUCCESS, "", graphinfo);

        } else if (graph.equals("player_player")) {
            if (lowerMatches == null)
                conditions.put("lowerMatches", PlayerPlayerGraph.lowestMatches);
            else
                conditions.put("lowerMatches",
                        lowerMatches > PlayerPlayerGraph.lowestMatches ? lowerMatches : PlayerPlayerGraph.lowestMatches);

            JSONObject graphinfo;
            Graph<JSONObject, JSONObject> subgraph = playerPlayerGraph.getGraph(conditions);
            graphinfo = playerPlayerGraph.graph2JSON(subgraph);
//            if (stronglyConnected != null && stronglyConnected) {
//                graphinfo = playerPlayerGraph.graph2JSON(playerPlayerGraph.getStronglyConnectedComponents(subgraph));
//            }
//            else
//                graphinfo = playerPlayerGraph.graph2JSON(subgraph);
            return new ResultMsg(ResultMsg.SUCCESS, "", graphinfo);
        }
        return new ResultMsg(ResultMsg.FAILURE, "graph not exists");
    }

    @GetMapping("/get_degrees")
    public ResultMsg getDegrees(@RequestParam String graph,
                                @RequestParam(required = false) Integer lowerWins,
                                @RequestParam(required = false) Integer lowerMatches) {
        Map<String, Object> conditions = new HashMap<>();
        if (lowerWins != null) conditions.put("lowerWins", lowerWins);
        if (lowerMatches != null) conditions.put("lowerMatches", lowerMatches);

        if (graph.equals("player_hero")) {
            JSONObject degreeinfo = playerHeroGraph.getDegrees(conditions);
            return new ResultMsg(ResultMsg.SUCCESS, "", degreeinfo);
        } else if (graph.equals("player_player")) {
            List<JSONObject> degreeinfo = playerPlayerGraph.getDegrees(conditions);
            return new ResultMsg(ResultMsg.SUCCESS, "", degreeinfo);
        }
        return new ResultMsg(ResultMsg.FAILURE, "graph not exists");
    }

    @GetMapping("/get_trianglecount")
    public ResultMsg getTriangleCount(@RequestParam String graph,
                                      @RequestParam(required = false) Integer lowerWins,
                                      @RequestParam(required = false) Integer lowerMatches) {
        Map<String, Object> conditions = new HashMap<>();
        if (lowerWins != null) conditions.put("lowerWins", lowerWins);
        if (lowerMatches != null) conditions.put("lowerMatches", lowerMatches);

        if (graph.equals("player_player")) {
            List<JSONObject> trianglecount = playerPlayerGraph.getTriangleCount(conditions);
            return new ResultMsg(ResultMsg.SUCCESS, "", trianglecount);
        }
        return new ResultMsg(ResultMsg.FAILURE, "graph not exists");
    }
}
