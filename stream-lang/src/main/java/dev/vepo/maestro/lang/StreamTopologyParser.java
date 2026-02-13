package dev.vepo.maestro.lang;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.lang.model.StreamQuery;

public class StreamTopologyParser {
	private static final Logger logger = LoggerFactory.getLogger(StreamTopologyParser.class);

	public List<StreamQuery> parse(String contents) {
		logger.debug("Parsing: {}", contents);
		var parser = new StreamParser(new CommonTokenStream(new StreamLexer(CharStreams.fromString(contents))));
		var walker = new ParseTreeWalker();
		var builder = new StreamQueriesBuilder();
		walker.walk(builder, parser.streamQueries());
		return builder.getQueries();
	}
}
