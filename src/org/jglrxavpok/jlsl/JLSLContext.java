package org.jglrxavpok.jlsl;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public class JLSLContext
{

	public static JLSLContext	 currentInstance;
	private CodeDecoder		   decoder;
	private CodeEncoder		   encoder;
	private ArrayList<CodeFilter> filters;
	private Object				object;

	public JLSLContext(CodeDecoder decoder, CodeEncoder encoder)
	{
		JLSLContext.currentInstance = this;
		this.filters = new ArrayList<CodeFilter>();
		this.decoder = decoder;
		this.decoder.context = this;
		this.encoder = encoder;
		this.encoder.context = this;
	}

	public void addFilters(CodeFilter... filters)
	{
		for(CodeFilter filter : filters)
			this.filters.add(filter);
	}

	public void requestAnalysisForEncoder(Object data)
	{
		this.object = data;
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		ArrayList<CodeFragment> finalFragments = new ArrayList<CodeFragment>();
		for(CodeFragment frag : fragments)
		{
			if(frag != null) finalFragments.add(filter(frag));
		}
		encoder.onRequestResult(finalFragments);
	}

	private CodeFragment filter(CodeFragment fragment)
	{
		for(CodeFilter filter : filters)
		{
			fragment = filter.filter(fragment);
		}
		return fragment;
	}

	public void execute(Object data, PrintWriter out)
	{
		this.object = data;
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		ArrayList<CodeFragment> finalFragments = new ArrayList<CodeFragment>();
		for(CodeFragment frag : fragments)
		{
			if(frag != null) finalFragments.add(filter(frag));
		}
		encoder.createSourceCode(finalFragments, out);
	}

	public Object getCurrentObject()
	{
		return object;
	}
}
