import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import org.xml.sax.InputSource;

public class NdlSearchRequest {
	public static void main(String[] args) throws Exception {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	public NdlSearchRequest() {
		
	}

	public String fetchNdlSearch(String query) {
		String apiUrl = "https://ndlsearch.ndl.go.jp/api/sru?operation=searchRetrieve&version=1.2&recordSchema=dcndl&onlyBib=true&recordPacking=xml&query="
				+ query;
		StringBuilder response = new StringBuilder();

		try {
			URL url = new URL(apiUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response.toString();
	}

	public String extractRdfRDF(String xmlString) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

		NodeList rdfNodes = doc.getElementsByTagName("rdf:RDF");
		if (rdfNodes.getLength() == 1) {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(rdfNodes.item(0)), new StreamResult(writer));
			return writer.toString();
		}else if(rdfNodes.getLength() == 0){
			System.out.println("not exist record");
			
		}else {
			System.out.println("exist multiple records");
		}
		return null;
	}

	// jena
	public Model loadRDFFromString(String rdfString) {
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader(rdfString), null, "RDF/XML");
		return model;
	}

	public void executeQuery(Model model, String queryString) {
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(System.out, results, query);
		}
	}
}