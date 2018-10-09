package org.librairy.service.graph.controllers;

import io.swagger.annotations.*;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.graph.facade.model.GraphService;
import org.librairy.service.graph.facade.rest.model.GraphInfo;
import org.librairy.service.graph.facade.rest.model.GraphRequest;
import org.librairy.service.graph.facade.rest.model.GraphSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/graphs")
@Api(tags="/graphs", description="manage graphs")
public class RestGraphsController {

    private static final Logger LOG = LoggerFactory.getLogger(RestGraphsController.class);

    @Autowired
    GraphService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "request a new graph", nickname = "postNewGraph", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = String.class),
    })
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> newGraph(@RequestBody GraphRequest request)  {
        try {
            String result = service.newGraph(request);
            return new ResponseEntity(result, HttpStatus.ACCEPTED);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down", HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            return new ResponseEntity("internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ApiOperation(value = "metadata of a graph", nickname = "getGraph", response=GraphInfo.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = GraphInfo.class),
    })
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<GraphInfo> getGraph(
            @ApiParam(value = "id", required = true) @PathVariable String id
    )  {
        try {
            GraphInfo result = new GraphInfo(service.getGraph(id));
            return new ResponseEntity(result, HttpStatus.OK);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down", HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            return new ResponseEntity("internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "list of graphs", nickname = "getGraphs", response=List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = List.class),
    })
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<GraphSummary> getGraphs(
    )  {
        try {
            List<GraphSummary> result = service.listGraphs().stream().map(gf -> new GraphSummary(gf)).collect(Collectors.toList());
            return new ResponseEntity(result, HttpStatus.OK);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down", HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            return new ResponseEntity("internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "remove a graph", nickname = "removeGraph", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = String.class),
    })
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<String> removeGraph(
            @ApiParam(value = "id", required = true) @PathVariable String id
    )  {
        try {
            String result = service.removeGraph(id);
            return new ResponseEntity(result, HttpStatus.OK);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down", HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            return new ResponseEntity("internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "remove all graphs", nickname = "removeGraphs", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = String.class),
    })
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<String> removeGraphs(
    )  {
        try {
            String result = service.removeGraphs();
            return new ResponseEntity(result, HttpStatus.OK);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down", HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            return new ResponseEntity("internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
