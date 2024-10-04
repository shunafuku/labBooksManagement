import java.io.StringReader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class JenaNTriplesTest {
    public static void main(String[] args) {
        // N-Triplesデータを文字列として定義
        String ntriples = 
                "<http://example.org/subject> <http://example.org/predicate> \"Object\" .\n" +
                "<http://example.org/subject> <http://example.org/anotherPredicate> \"Another Object\" .";

            // モデルを作成
            Model model = ModelFactory.createDefaultModel();

            // 文字列からN-Triplesを読み込む
            RDFDataMgr.read(model, new StringReader(ntriples), null, Lang.NTRIPLES);

            // モデルの内容を表示
            System.out.println("モデルの内容:");
            model.write(System.out, "TURTLE");
    }
}