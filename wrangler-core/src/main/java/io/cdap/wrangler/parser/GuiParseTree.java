package io.cdap.wrangler.parser;

/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.antlr.v4.gui.Trees;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Demonstrates how to use ANTLR's Trees GUI to visualize a parse tree
 * for Wrangler's DSL using the Directives grammar.
 */
public class GuiParseTree {
    public static void main(String[] args) throws Exception {
        String input = "\"aggregate-stats :data_transfer_size response_time total_size_mb\n" +
                "total_time_sec\"";

        // Step 1: Create CharStream from input
        CharStream cs = CharStreams.fromString(input);

        // Step 2: Create lexer and parser
        DirectivesLexer lexer = new DirectivesLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DirectivesParser parser = new DirectivesParser(tokens);

        // Optional: print tokens
        tokens.fill();
        for (Token token : tokens.getTokens()) {
            System.out.println(token);
        }

        // Step 3: Parse using a start rule (e.g., recipe)
        ParseTree tree = parser.recipe();

        Trees.inspect(tree, parser);  // Launches GUI!
    }
}
