package org.jglrxavpok.jlsl;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public class JLSLContext
{

	private CodeDecoder decoder;
	private CodeEncoder encoder;

	public JLSLContext(CodeDecoder decoder, CodeEncoder encoder)
	{
		this.decoder = decoder;
		this.decoder.context = this;
		this.encoder = encoder;
		this.encoder.context = this;
	}
	
	public void requestAnalysisForEncoder(Object data)
	{
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		encoder.onRequestResult(fragments);
	}
	
	public void execute(Object data, PrintWriter out)
	{
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		encoder.createSourceCode(fragments, out);
	}
}
